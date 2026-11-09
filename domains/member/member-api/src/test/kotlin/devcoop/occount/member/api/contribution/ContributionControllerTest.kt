package devcoop.occount.member.api.contribution

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.member.api.support.FakeUserRepository
import devcoop.occount.member.api.support.mockMvc
import devcoop.occount.member.application.output.ContributionAccountRepository
import devcoop.occount.member.application.output.ContributionDepositRequestRepository
import devcoop.occount.member.application.output.ContributionTransactionRepository
import devcoop.occount.member.application.output.ContributionWithdrawalRequestRepository
import devcoop.occount.member.application.output.PageResult
import devcoop.occount.member.application.query.GetContributionDepositRequestsQueryService
import devcoop.occount.member.application.query.GetContributionWithdrawalRequestsQueryService
import devcoop.occount.member.application.query.GetMyContributionAccountQueryService
import devcoop.occount.member.application.query.GetMyContributionDepositRequestsQueryService
import devcoop.occount.member.application.query.GetMyContributionWithdrawalRequestsQueryService
import devcoop.occount.member.application.usecase.contribution.ApproveContributionDepositRequestUseCase
import devcoop.occount.member.application.usecase.contribution.ApproveContributionWithdrawalRequestUseCase
import devcoop.occount.member.application.usecase.contribution.RejectContributionDepositRequestUseCase
import devcoop.occount.member.application.usecase.contribution.RejectContributionWithdrawalRequestUseCase
import devcoop.occount.member.application.usecase.contribution.SubmitContributionDepositRequestUseCase
import devcoop.occount.member.application.usecase.contribution.SubmitContributionWithdrawalRequestUseCase
import devcoop.occount.member.domain.contribution.ContributionAccount
import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus
import devcoop.occount.member.domain.contribution.ContributionTransaction
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequest
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequestStatus
import devcoop.occount.member.domain.contribution.ContributionTransactionType
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("ContributionController 웹 테스트")
class ContributionControllerTest {
    @Test
    @DisplayName("출자금 납입 신청을 생성하면 201과 신청 정보를 반환한다")
    fun `submitDepositRequest returns created request`() {
        val fixture = Fixture()

        fixture.mockMvc.perform(
            post("/contributions/deposit-requests")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"amount": 10000, "depositor_name": "홍길동", "bank_name": "국민은행"}"""),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.amount").value(10000))
            .andExpect(jsonPath("$.depositor_name").value("홍길동"))
            .andExpect(jsonPath("$.bank_name").value("국민은행"))
            .andExpect(jsonPath("$.processed_by").isEmpty)
            .andExpect(jsonPath("$.processed_at").isEmpty)
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    @DisplayName("내 출자금 납입 신청 목록을 조회한다")
    fun `getMyDepositRequests returns my requests`() {
        val fixture = Fixture()
        fixture.depositRepository.save(ContributionDepositRequest.request(userId = 1L, amount = 10000))
        fixture.depositRepository.save(ContributionDepositRequest.request(userId = 2L, amount = 20000))

        fixture.mockMvc.perform(
            get("/contributions/deposit-requests/my")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.requests.length()").value(1))
            .andExpect(jsonPath("$.requests[0].amount").value(10000))
    }

    @Test
    @DisplayName("관리자가 상태별 출자금 납입 신청 목록을 조회한다")
    fun `getDepositRequests filters by status`() {
        val fixture = Fixture()
        fixture.depositRepository.save(ContributionDepositRequest.request(userId = 1L, amount = 10000))
        fixture.depositRepository.save(
            ContributionDepositRequest.request(userId = 2L, amount = 20000)
                .copy(status = ContributionDepositRequestStatus.APPROVED),
        )

        fixture.mockMvc.perform(
            get("/contributions/deposit-requests")
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "10"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.requests.length()").value(1))
            .andExpect(jsonPath("$.requests[0].status").value("PENDING"))
            .andExpect(jsonPath("$.page.total_count").value(1))
    }

    @Test
    @DisplayName("관리자가 출자금 납입 신청을 승인한다")
    fun `approveDepositRequest approves request`() {
        val fixture = Fixture()
        val savedRequest = fixture.depositRepository.save(
            ContributionDepositRequest.request(userId = 1L, amount = 10000),
        )

        fixture.mockMvc.perform(
            patch("/contributions/deposit-requests/${savedRequest.id}/approve")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "42"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.processed_by").value(42))
            .andExpect(jsonPath("$.processed_at").isNotEmpty)

        assertEquals(10000L, fixture.accountRepository.findByUserId(1L)?.balance)
        val transaction = fixture.transactionRepository.findAllByUserId(1L).single()
        assertEquals(ContributionTransactionType.DEPOSIT, transaction.type)
        assertEquals(10000L, transaction.amount)
        assertEquals(0L, transaction.beforeBalance)
        assertEquals(10000L, transaction.afterBalance)
        assertEquals(savedRequest.id, transaction.sourceRequestId)
        assertEquals(42L, fixture.depositRepository.findById(savedRequest.id)?.processedBy)
    }

    @Test
    @DisplayName("내 출자금 계좌를 조회한다")
    fun `getMyAccount returns my contribution account`() {
        val fixture = Fixture(
            accountRepository = FakeContributionAccountRepository(
                listOf(ContributionAccount(userId = 1L, balance = 10000)),
            ),
        )

        fixture.mockMvc.perform(
            get("/contributions/account/my")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.balance").value(10000))
    }

    @Test
    @DisplayName("출자금 반환 신청을 생성한다")
    fun `submitWithdrawalRequest returns created request`() {
        val fixture = Fixture()

        fixture.mockMvc.perform(
            post("/contributions/withdrawal-requests")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"amount": 5000, "bank_name": "국민은행", "account_number": "123"}"""),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.amount").value(5000))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.bank_name").value("국민은행"))
            .andExpect(jsonPath("$.account_number").value("123"))
            .andExpect(jsonPath("$.processed_by").isEmpty)
            .andExpect(jsonPath("$.processed_at").isEmpty)
    }

    @Test
    @DisplayName("관리자가 상태별 출자금 반환 신청 목록을 조회한다")
    fun `getWithdrawalRequests filters by status`() {
        val fixture = Fixture()
        fixture.withdrawalRepository.save(ContributionWithdrawalRequest.request(userId = 1L, amount = 5000))
        fixture.withdrawalRepository.save(
            ContributionWithdrawalRequest.request(userId = 2L, amount = 7000)
                .copy(status = ContributionWithdrawalRequestStatus.REJECTED),
        )

        fixture.mockMvc.perform(
            get("/contributions/withdrawal-requests")
                .param("status", "PENDING"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.requests.length()").value(1))
            .andExpect(jsonPath("$.requests[0].status").value("PENDING"))
    }

    @Test
    @DisplayName("관리자가 출자금 반환 신청을 승인한다")
    fun `approveWithdrawalRequest approves request`() {
        val fixture = Fixture(
            accountRepository = FakeContributionAccountRepository(
                listOf(ContributionAccount(userId = 1L, balance = 10000)),
            ),
        )
        val savedRequest = fixture.withdrawalRepository.save(
            ContributionWithdrawalRequest.request(userId = 1L, amount = 5000),
        )

        fixture.mockMvc.perform(
            patch("/contributions/withdrawal-requests/${savedRequest.id}/approve")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "42"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPROVED"))
            .andExpect(jsonPath("$.processed_by").value(42))
            .andExpect(jsonPath("$.processed_at").isNotEmpty)

        assertEquals(5000L, fixture.accountRepository.findByUserId(1L)?.balance)
        val transaction = fixture.transactionRepository.findAllByUserId(1L).single()
        assertEquals(ContributionTransactionType.WITHDRAWAL, transaction.type)
        assertEquals(5000L, transaction.amount)
        assertEquals(10000L, transaction.beforeBalance)
        assertEquals(5000L, transaction.afterBalance)
        assertEquals(savedRequest.id, transaction.sourceRequestId)
        assertEquals(42L, fixture.withdrawalRepository.findById(savedRequest.id)?.processedBy)
    }

    @Test
    fun `deposit JSON accepts amounts beyond integer range`() {
        val fixture = Fixture()
        fixture.mockMvc.perform(
            post("/contributions/deposit-requests")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"amount":3000000000,"depositor_name":"Depositor","bank_name":"Bank"}"""),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.amount").value(3000000000L))
            .andExpect(jsonPath("$.depositor_name").value("Depositor"))
        assertEquals(3000000000L, fixture.depositRepository.findAllByUserId(1L).single().amount)
    }

    @Test
    fun `duplicate processing returns conflict without another balance change`() {
        val fixture = Fixture()
        val deposit = fixture.depositRepository.save(ContributionDepositRequest.request(1L, 10000L))
        val withdrawal = fixture.withdrawalRepository.save(ContributionWithdrawalRequest.request(1L, 5000L))
        for (path in listOf("deposit-requests/${deposit.id}", "withdrawal-requests/${withdrawal.id}")) {
            repeat(2) { attempt ->
                fixture.mockMvc.perform(
                    patch("/contributions/$path/approve")
                        .header(AuthHeaders.AUTHENTICATED_USER_ID, "42"),
                ).andExpect(if (attempt == 0) status().isOk else status().isConflict)
            }
        }
        assertEquals(5000L, fixture.accountRepository.findByUserId(1L)?.balance)
        assertEquals(2, fixture.transactionRepository.findAllByUserId(1L).size)
    }

    @Test
    fun `insufficient withdrawal returns bad request and leaves state unchanged`() {
        val fixture = Fixture(
            accountRepository = FakeContributionAccountRepository(listOf(ContributionAccount(userId = 1L, balance = 1000L))),
        )
        val request = fixture.withdrawalRepository.save(ContributionWithdrawalRequest.request(1L, 5000L))
        fixture.mockMvc.perform(
            patch("/contributions/withdrawal-requests/${request.id}/approve")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "42"),
        ).andExpect(status().isBadRequest)
        assertEquals(1000L, fixture.accountRepository.findByUserId(1L)?.balance)
        assertEquals(ContributionWithdrawalRequestStatus.PENDING, fixture.withdrawalRepository.findById(request.id)?.status)
        assertEquals(emptyList(), fixture.transactionRepository.findAllByUserId(1L))
    }

    @Test
    fun `request optimistic locking superclass returns retryable conflict`() {
        val fixture = Fixture()
        val request = fixture.depositRepository.save(ContributionDepositRequest.request(1L, 10000L))
        fixture.depositRepository.saveFailure = OptimisticLockingFailureException("stale request")
        fixture.mockMvc.perform(
            patch("/contributions/deposit-requests/${request.id}/approve")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "42"),
        ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Contribution data changed concurrently. Please retry the request."))
    }

    @Test
    fun `account optimistic locking subclass returns retryable conflict`() {
        val fixture = Fixture(
            accountRepository = FakeContributionAccountRepository(
                listOf(ContributionAccount(userId = 1L, balance = 10000L)),
                ObjectOptimisticLockingFailureException(ContributionAccount::class.java, 1L),
            ),
        )
        val request = fixture.withdrawalRepository.save(ContributionWithdrawalRequest.request(1L, 5000L))
        fixture.mockMvc.perform(
            patch("/contributions/withdrawal-requests/${request.id}/approve")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "42"),
        ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Contribution data changed concurrently. Please retry the request."))
        assertEquals(emptyList(), fixture.transactionRepository.findAllByUserId(1L))
    }

    @Test
    fun `reject deposit records authenticated processor and reason`() {
        val fixture = Fixture()
        val request = fixture.depositRepository.save(ContributionDepositRequest.request(1L, 10000L))
        fixture.mockMvc.perform(
            patch("/contributions/deposit-requests/${request.id}/reject")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "42")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"reason":"Unverified deposit"}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.rejection_reason").value("Unverified deposit"))
            .andExpect(jsonPath("$.processed_by").value(42))
            .andExpect(jsonPath("$.processed_at").isNotEmpty)
        assertNotNull(fixture.depositRepository.findById(request.id)?.processedAt)
        assertEquals(emptyList(), fixture.transactionRepository.findAllByUserId(1L))
    }

    @Test
    fun `reject withdrawal without body records authenticated processor`() {
        val fixture = Fixture()
        val request = fixture.withdrawalRepository.save(ContributionWithdrawalRequest.request(1L, 5000L))
        fixture.mockMvc.perform(
            patch("/contributions/withdrawal-requests/${request.id}/reject")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "42"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REJECTED"))
            .andExpect(jsonPath("$.processed_by").value(42))
            .andExpect(jsonPath("$.processed_at").isNotEmpty)
        assertNotNull(fixture.withdrawalRepository.findById(request.id)?.processedAt)
        assertEquals(emptyList(), fixture.transactionRepository.findAllByUserId(1L))
    }

    @Test
    fun `approval without authenticated principal returns unauthorized`() {
        Fixture().mockMvc.perform(patch("/contributions/deposit-requests/1/approve"))
            .andExpect(status().isUnauthorized)
    }

    private class Fixture(
        val depositRepository: FakeContributionDepositRequestRepository = FakeContributionDepositRequestRepository(),
        val withdrawalRepository: FakeContributionWithdrawalRequestRepository = FakeContributionWithdrawalRequestRepository(),
        val accountRepository: FakeContributionAccountRepository = FakeContributionAccountRepository(),
        val transactionRepository: FakeContributionTransactionRepository = FakeContributionTransactionRepository(),
    ) {
        private val userRepository = FakeUserRepository()
        val mockMvc = mockMvc(
            ContributionController(
                submitContributionDepositRequestUseCase = SubmitContributionDepositRequestUseCase(depositRepository),
                getContributionDepositRequestsQueryService = GetContributionDepositRequestsQueryService(
                    depositRepository,
                    userRepository,
                ),
                getMyContributionDepositRequestsQueryService = GetMyContributionDepositRequestsQueryService(depositRepository),
                getMyContributionAccountQueryService = GetMyContributionAccountQueryService(accountRepository),
                approveContributionDepositRequestUseCase = ApproveContributionDepositRequestUseCase(
                    depositRepository,
                    accountRepository,
                    transactionRepository,
                ),
                rejectContributionDepositRequestUseCase = RejectContributionDepositRequestUseCase(depositRepository),
                submitContributionWithdrawalRequestUseCase = SubmitContributionWithdrawalRequestUseCase(withdrawalRepository),
                getContributionWithdrawalRequestsQueryService = GetContributionWithdrawalRequestsQueryService(
                    withdrawalRepository,
                    userRepository,
                ),
                getMyContributionWithdrawalRequestsQueryService = GetMyContributionWithdrawalRequestsQueryService(
                    withdrawalRepository,
                ),
                approveContributionWithdrawalRequestUseCase = ApproveContributionWithdrawalRequestUseCase(
                    withdrawalRepository,
                    accountRepository,
                    transactionRepository,
                ),
                rejectContributionWithdrawalRequestUseCase = RejectContributionWithdrawalRequestUseCase(withdrawalRepository),
            ),
        )
    }

    private class FakeContributionDepositRequestRepository : ContributionDepositRequestRepository {
        private val requestsById = linkedMapOf<Long, ContributionDepositRequest>()
        private var nextId = 1L
        var saveFailure: OptimisticLockingFailureException? = null

        override fun findById(id: Long): ContributionDepositRequest? = requestsById[id]

        override fun save(request: ContributionDepositRequest): ContributionDepositRequest {
            saveFailure?.let { throw it }
            val persistedRequest = if (request.id == 0L) request.copy(id = nextId++) else request
            requestsById[persistedRequest.id] = persistedRequest
            return persistedRequest
        }

        override fun findPage(
            status: ContributionDepositRequestStatus?,
            page: Int,
            size: Int,
        ): PageResult<ContributionDepositRequest> {
            val requests = requestsById.values
                .filter { status == null || it.status == status }
                .sortedByDescending { it.requestedAt }
            return PageResult(
                content = requests.drop(page * size).take(size),
                page = page,
                size = size,
                totalCount = requests.size,
                totalPages = if (requests.isEmpty()) 0 else (requests.size + size - 1) / size,
            )
        }

        override fun findAllByUserId(userId: Long): List<ContributionDepositRequest> {
            return requestsById.values.filter { it.userId == userId }.sortedByDescending { it.requestedAt }
        }
    }

    private class FakeContributionWithdrawalRequestRepository : ContributionWithdrawalRequestRepository {
        private val requestsById = linkedMapOf<Long, ContributionWithdrawalRequest>()
        private var nextId = 1L

        override fun findById(id: Long): ContributionWithdrawalRequest? = requestsById[id]

        override fun save(request: ContributionWithdrawalRequest): ContributionWithdrawalRequest {
            val persistedRequest = if (request.id == 0L) request.copy(id = nextId++) else request
            requestsById[persistedRequest.id] = persistedRequest
            return persistedRequest
        }

        override fun findPage(
            status: ContributionWithdrawalRequestStatus?,
            page: Int,
            size: Int,
        ): PageResult<ContributionWithdrawalRequest> {
            val requests = requestsById.values
                .filter { status == null || it.status == status }
                .sortedByDescending { it.requestedAt }
            return PageResult(
                content = requests.drop(page * size).take(size),
                page = page,
                size = size,
                totalCount = requests.size,
                totalPages = if (requests.isEmpty()) 0 else (requests.size + size - 1) / size,
            )
        }

        override fun findAllByUserId(userId: Long): List<ContributionWithdrawalRequest> {
            return requestsById.values.filter { it.userId == userId }.sortedByDescending { it.requestedAt }
        }
    }

    private class FakeContributionAccountRepository(
        initialAccounts: List<ContributionAccount> = emptyList(),
        private val saveFailure: OptimisticLockingFailureException? = null,
    ) : ContributionAccountRepository {
        private val accountsByUserId = linkedMapOf<Long, ContributionAccount>().apply {
            initialAccounts.forEach { account -> put(account.userId, account) }
        }

        override fun findByUserIdForUpdate(userId: Long): ContributionAccount? = findByUserId(userId)

        override fun promoteOwnerToMember(userId: Long) = Unit

        override fun findByUserId(userId: Long): ContributionAccount? = accountsByUserId[userId]

        override fun save(account: ContributionAccount): ContributionAccount {
            saveFailure?.let { throw it }
            accountsByUserId[account.userId] = account
            return account
        }
    }

    private class FakeContributionTransactionRepository : ContributionTransactionRepository {
        private val transactions = mutableListOf<ContributionTransaction>()

        override fun save(transaction: ContributionTransaction): ContributionTransaction {
            val persisted = if (transaction.id == 0L) transaction.copy(id = transactions.size + 1L) else transaction
            transactions += persisted
            return persisted
        }

        override fun findAllByUserId(userId: Long): List<ContributionTransaction> {
            return transactions.filter { it.userId == userId }
        }
    }
}
