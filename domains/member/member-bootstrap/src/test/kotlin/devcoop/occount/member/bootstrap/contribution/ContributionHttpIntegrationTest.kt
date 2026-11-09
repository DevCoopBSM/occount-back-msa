package devcoop.occount.member.bootstrap.contribution

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.member.api.contribution.ContributionController
import devcoop.occount.member.api.support.ApiAdviceHandler
import devcoop.occount.member.application.output.ContributionWithdrawalRequestRepository
import devcoop.occount.member.application.query.*
import devcoop.occount.member.application.usecase.contribution.*
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequest
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequestStatus
import devcoop.occount.member.domain.user.Role
import devcoop.occount.member.infrastructure.crypto.CryptoConverter
import devcoop.occount.member.infrastructure.crypto.CryptoHelper
import devcoop.occount.member.infrastructure.persistence.UserJpaEntity
import devcoop.occount.member.infrastructure.persistence.UserJpaRepository
import devcoop.occount.member.infrastructure.persistence.UserRepositoryImpl
import devcoop.occount.member.infrastructure.persistence.contribution.*
import devcoop.occount.web.auth.AuthWebMvcConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.UUID
import javax.sql.DataSource

/**
 * By default this covers the member boundary with trusted headers, not gateway JWT validation.
 * CONTRIBUTION_GATEWAY_URL and CONTRIBUTION_GATEWAY_JWT_SECRET enable a real running gateway.
 * CONTRIBUTION_HTTP_PORT pins its backend port; default 0 keeps the embedded server random-port.
 * CONTRIBUTION_HTTP_DB_URL/USERNAME/PASSWORD opt into a dedicated, disposable MySQL schema.
 */
@SpringBootTest(
    classes = [ContributionHttpTestConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "spring.config.name=contribution-http-test",
        "server.servlet.context-path=/api/v3",
        "spring.jackson.property-naming-strategy=SNAKE_CASE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.datasource.hikari.maximum-pool-size=4",
    ],
)
class ContributionHttpIntegrationTest @Autowired constructor(
    private val mapper: ObjectMapper,
    private val users: UserJpaRepository,
    private val accounts: ContributionAccountPersistenceRepository,
    private val withdrawals: ContributionWithdrawalRequestPersistenceRepository,
    private val ledger: ContributionTransactionPersistenceRepository,
    private val commitConflict: CommitConflictWithdrawalRepository,
    private val approveDeposit: ApproveContributionDepositRequestUseCase,
    private val approveWithdrawal: ApproveContributionWithdrawalRequestUseCase,
) {
    @LocalServerPort
    private var port: Int = 0
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private var ownerId = 0L
    private var adminId = 0L
    private val gatewayUrl = System.getenv("CONTRIBUTION_GATEWAY_URL")

    @BeforeEach
    fun createIdentities() {
        ownerId = users.saveAndFlush(UserJpaEntity(email = "${UUID.randomUUID()}@http.test")).id
        adminId = users.saveAndFlush(UserJpaEntity(email = "${UUID.randomUUID()}@http.test", role = Role.ROLE_ADMIN)).id
        commitConflict.reset()
        assertTrue(AopUtils.isAopProxy(approveDeposit), "Approval must run through the production transaction proxy")
        assertTrue(AopUtils.isAopProxy(approveWithdrawal))
    }

    @Test
    fun `trusted header principal submits admin approves and real HTTP reads committed balance`() {
        val submitted = deposit(3_000_000_000L)
        assertEquals(ownerId, submitted.path("user_id").asLong())
        assertEquals("HTTP depositor", submitted.path("depositor_name").asText())
        assertEquals("HTTP bank", submitted.path("bank_name").asText())
        assertFalse(submitted.has("userId"))
        val id = submitted.path("id").asLong()
        val approved = json(request("PATCH", "/deposit-requests/$id/approve", adminId), 200)
        assertEquals("APPROVED", approved.path("status").asText())
        assertEquals(adminId, approved.path("processed_by").asLong())
        assertFalse(approved.path("processed_at").isNull)
        assertEquals(3_000_000_000L, balance())
        assertEquals(Role.ROLE_MEMBER, users.findById(ownerId).orElseThrow().role)
        assertEquals(1, ledger.findAllByUserIdOrderByOccurredAtDesc(ownerId).size)

        json(request("PATCH", "/deposit-requests/$id/approve", adminId), 409)
        assertEquals(3_000_000_000L, balance())
        assertEquals(1, ledger.findAllByUserIdOrderByOccurredAtDesc(ownerId).size)
    }

    @Test
    fun `missing or malformed trusted identity is unauthorized rather than an invented principal`() {
        json(directRequest("GET", "/account/my"), 401)
        val invalid = HttpRequest.newBuilder(directUri("/account/my"))
            .header(AuthHeaders.AUTHENTICATED_USER_ID, "not-a-user-id").GET().build()
        json(client.send(invalid, HttpResponse.BodyHandlers.ofString()), 401)
        val jwtOnly = HttpRequest.newBuilder(directUri("/account/my"))
            .header("Authorization", "Bearer not-a-gateway-validated-token").GET().build()
        json(client.send(jwtOnly, HttpResponse.BodyHandlers.ofString()), 401)
    }

    @Test
    fun `optional real gateway verifies JWT enforces admin policy and replaces spoofed identity`() {
        org.junit.jupiter.api.Assumptions.assumeTrue(gatewayUrl != null, "External gateway is opt-in")
        assertEquals(401, request("GET", "/account/my").statusCode())
        val invalid = HttpRequest.newBuilder(uri("/account/my"))
            .header("Authorization", "Bearer invalid-token").GET().build()
        assertEquals(401, client.send(invalid, HttpResponse.BodyHandlers.ofString()).statusCode())
        val id = deposit(100L).path("id").asLong()
        assertEquals(403, request("GET", "/deposit-requests", ownerId).statusCode())
        assertEquals(403, request("PATCH", "/deposit-requests/$id/approve", ownerId).statusCode())
        json(request("GET", "/deposit-requests", adminId), 200)
        // Every authenticated gateway request also carries spoofed identity 999 below.
        val approved = json(request("PATCH", "/deposit-requests/$id/approve", adminId), 200)
        assertEquals(adminId, approved.path("processed_by").asLong())
        assertEquals(100L, balance())
    }

    @Test
    fun `insufficient withdrawal changes neither account request audit nor ledger`() {
        fund(100L)
        val id = withdrawal(101L)
        val before = withdrawals.findById(id).orElseThrow()
        json(request("PATCH", "/withdrawal-requests/$id/approve", adminId), 400)
        assertEquals(100L, balance())
        val pending = withdrawals.findById(id).orElseThrow()
        assertEquals(ContributionWithdrawalRequestStatus.PENDING, pending.status)
        assertEquals(before.version, pending.version)
        assertNull(pending.processedAt)
        assertNull(pending.processedBy)
        assertEquals(1, ledger.findAllByUserIdOrderByOccurredAtDesc(ownerId).size)
    }

    @Test
    fun `database optimistic failure at Spring transaction commit returns 409 and rolls back all approval writes`() {
        fund(100L)
        val id = withdrawal(40L)
        val accountBefore = accounts.findById(ownerId).orElseThrow()
        val requestBefore = withdrawals.findById(id).orElseThrow()
        val ledgerBefore = ledger.findAllByUserIdOrderByOccurredAtDesc(ownerId).map { it.id }
        commitConflict.arm(id)

        val failure = json(request("PATCH", "/withdrawal-requests/$id/approve", adminId), 409)
        assertEquals("Contribution data changed concurrently. Please retry the request.", failure.path("message").asText())
        assertTrue(commitConflict.saveReturned, "Real repository save must return without throwing")
        assertTrue(commitConflict.beforeCommitReached, "Failure must arise at commit, not in the use case")
        assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, commitConflict.completionStatus)
        assertEquals(100L, balance())
        assertEquals(accountBefore.version, accounts.findById(ownerId).orElseThrow().version)
        val pending = withdrawals.findById(id).orElseThrow()
        assertEquals(ContributionWithdrawalRequestStatus.PENDING, pending.status)
        assertEquals(requestBefore.version!! + 1L, pending.version, "Only the independent writer's version bump survives")
        assertNull(pending.processedAt)
        assertNull(pending.processedBy)
        assertEquals(ledgerBefore, ledger.findAllByUserIdOrderByOccurredAtDesc(ownerId).map { it.id })
    }

    private fun deposit(amount: Long): JsonNode = json(
        request("POST", "/deposit-requests", ownerId,
            """{"amount":$amount,"depositor_name":"HTTP depositor","bank_name":"HTTP bank"}"""), 201,
    )

    private fun fund(amount: Long) {
        val id = deposit(amount).path("id").asLong()
        json(request("PATCH", "/deposit-requests/$id/approve", adminId), 200)
    }

    private fun withdrawal(amount: Long): Long = json(
        request("POST", "/withdrawal-requests", ownerId,
            """{"amount":$amount,"bank_name":"HTTP bank","account_number":"123","account_holder":"Owner"}"""), 201,
    ).also { assertEquals("123", it.path("account_number").asText()) }.path("id").asLong()

    private fun balance(): Long = json(request("GET", "/account/my", ownerId), 200).path("balance").asLong()
    private fun directUri(path: String) = URI.create("http://127.0.0.1:$port/api/v3/contributions$path")
    private fun uri(path: String) = gatewayUrl?.let { URI.create("${it.trimEnd('/')}/api/v3/contributions$path") }
        ?: directUri(path)

    private fun request(method: String, path: String, userId: Long? = null, body: String? = null): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(30))
        userId?.let {
            if (gatewayUrl == null) {
                builder.header(AuthHeaders.AUTHENTICATED_USER_ID, it.toString())
            } else {
                builder.header("Authorization", "Bearer ${token(it)}")
                builder.header(AuthHeaders.AUTHENTICATED_USER_ID, "999")
            }
        }
        if (body != null) builder.header("Content-Type", "application/json")
        builder.method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun directRequest(method: String, path: String): HttpResponse<String> = client.send(
        HttpRequest.newBuilder(directUri(path)).method(method, HttpRequest.BodyPublishers.noBody()).build(),
        HttpResponse.BodyHandlers.ofString(),
    )

    private fun token(userId: Long): String {
        val key = requireNotNull(System.getenv("CONTRIBUTION_GATEWAY_JWT_SECRET")) {
            "CONTRIBUTION_GATEWAY_JWT_SECRET (plaintext HS256 secret) is required with CONTRIBUTION_GATEWAY_URL"
        }
        val encoder = Base64.getUrlEncoder().withoutPadding()
        fun encode(value: String) = encoder.encodeToString(value.toByteArray(Charsets.UTF_8))
        val role = if (userId == adminId) "ROLE_ADMIN" else "ROLE_USER"
        val unsigned = encode("""{"alg":"HS256","typ":"JWT"}""") + "." +
            encode("""{"sub":"$userId","role":"$role","exp":${Instant.now().epochSecond + 300}}""")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return "$unsigned.${encoder.encodeToString(mac.doFinal(unsigned.toByteArray(Charsets.US_ASCII)))}"
    }

    private fun json(response: HttpResponse<String>, expectedStatus: Int): JsonNode {
        assertEquals(expectedStatus, response.statusCode(), response.body())
        return mapper.readTree(response.body())
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun database(properties: DynamicPropertyRegistry) {
            properties.add("server.port") { System.getenv("CONTRIBUTION_HTTP_PORT") ?: "0" }
            properties.add("spring.datasource.url") {
                System.getenv("CONTRIBUTION_HTTP_DB_URL")
                    ?: "jdbc:h2:mem:contribution-http;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000"
            }
            properties.add("spring.datasource.username") { System.getenv("CONTRIBUTION_HTTP_DB_USERNAME") ?: "sa" }
            properties.add("spring.datasource.password") { System.getenv("CONTRIBUTION_HTTP_DB_PASSWORD") ?: "" }
        }
    }
}

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableTransactionManagement
@EntityScan(basePackageClasses = [UserJpaEntity::class, ContributionAccountJpaEntity::class])
@EnableJpaRepositories(basePackageClasses = [UserJpaRepository::class])
@Import(
    ContributionController::class, ApiAdviceHandler::class, AuthWebMvcConfig::class,
    SubmitContributionDepositRequestUseCase::class, SubmitContributionWithdrawalRequestUseCase::class,
    ApproveContributionDepositRequestUseCase::class, ApproveContributionWithdrawalRequestUseCase::class,
    RejectContributionDepositRequestUseCase::class, RejectContributionWithdrawalRequestUseCase::class,
    GetMyContributionAccountQueryService::class, GetMyContributionDepositRequestsQueryService::class,
    GetMyContributionWithdrawalRequestsQueryService::class, GetContributionDepositRequestsQueryService::class,
    GetContributionWithdrawalRequestsQueryService::class,
    ContributionAccountRepositoryImpl::class, ContributionDepositRequestRepositoryImpl::class,
    ContributionWithdrawalRequestRepositoryImpl::class, ContributionTransactionRepositoryImpl::class,
    UserRepositoryImpl::class,
)
class ContributionHttpTestConfiguration {
    @Bean
    fun cryptoHelper(): CryptoHelper = CryptoHelper("12345678901234567890123456789012").also(CryptoConverter::configure)

    @Bean
    @Primary
    fun commitConflictWithdrawalRepository(
        delegate: ContributionWithdrawalRequestRepositoryImpl,
        dataSource: DataSource,
    ) = CommitConflictWithdrawalRepository(delegate, dataSource)
}

/** Changes a real database version after JPA loaded it, never throws a synthetic exception. */
class CommitConflictWithdrawalRepository(
    private val delegate: ContributionWithdrawalRequestRepositoryImpl,
    private val dataSource: DataSource,
) : ContributionWithdrawalRequestRepository by delegate {
    @Volatile private var targetId: Long? = null
    @Volatile var saveReturned = false
        private set
    @Volatile var beforeCommitReached = false
        private set
    @Volatile var completionStatus: Int? = null
        private set

    fun reset() {
        targetId = null
        saveReturned = false
        beforeCommitReached = false
        completionStatus = null
    }

    fun arm(id: Long) { targetId = id }

    override fun findById(id: Long): ContributionWithdrawalRequest? {
        val loaded = delegate.findById(id)
        if (targetId == id && loaded != null) {
            check(TransactionSynchronizationManager.isActualTransactionActive())
            // A separate autocommit connection advances the committed version while the
            // current persistence context retains its stale managed request entity.
            dataSource.connection.use { connection ->
                check(connection.autoCommit)
                connection.prepareStatement("UPDATE contribution_withdrawal_request SET version = version + 1 WHERE id = ?").use {
                    it.setLong(1, id)
                    check(it.executeUpdate() == 1)
                }
            }
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun beforeCommit(readOnly: Boolean) { beforeCommitReached = true }
                override fun afterCompletion(status: Int) { completionStatus = status }
            })
        }
        return loaded
    }

    override fun save(request: ContributionWithdrawalRequest): ContributionWithdrawalRequest {
        val saved = delegate.save(request)
        if (request.id == targetId) saveReturned = true
        return saved
    }
}
