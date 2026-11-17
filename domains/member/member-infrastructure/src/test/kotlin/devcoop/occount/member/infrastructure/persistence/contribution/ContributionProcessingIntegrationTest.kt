package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.application.output.ContributionTransactionRepository
import devcoop.occount.member.application.usecase.contribution.*
import devcoop.occount.member.domain.contribution.*
import devcoop.occount.member.domain.user.Role
import devcoop.occount.member.infrastructure.crypto.CryptoConverter
import devcoop.occount.member.infrastructure.crypto.CryptoHelper
import devcoop.occount.member.infrastructure.persistence.UserJpaEntity
import devcoop.occount.member.infrastructure.persistence.UserJpaRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = [ContributionPersistenceVersionTestConfig::class])
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ContributionProcessingIntegrationTest @Autowired constructor(
    private val deposits: ContributionDepositRequestPersistenceRepository,
    private val withdrawals: ContributionWithdrawalRequestPersistenceRepository,
    private val accounts: ContributionAccountPersistenceRepository,
    private val ledger: ContributionTransactionPersistenceRepository,
    private val users: UserJpaRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val tx = TransactionTemplate(transactionManager)
    private val ownerIds = mutableListOf<Long>()
    private val depositRepository = ContributionDepositRequestRepositoryImpl(deposits)
    private val withdrawalRepository = ContributionWithdrawalRequestRepositoryImpl(withdrawals)
    private val accountRepository = ContributionAccountRepositoryImpl(accounts, users)
    private val ledgerRepository = ContributionTransactionRepositoryImpl(ledger)
    private val approveDeposit = ApproveContributionDepositRequestUseCase(depositRepository, accountRepository, ledgerRepository)
    private val approveWithdrawal = ApproveContributionWithdrawalRequestUseCase(withdrawalRepository, accountRepository, ledgerRepository)

    @BeforeEach
    fun configureEncryption() {
        CryptoConverter.configure(CryptoHelper("12345678901234567890123456789012"))
    }

    private fun owner(role: Role = Role.ROLE_USER): Long = tx.execute {
        users.saveAndFlush(UserJpaEntity(email = "${UUID.randomUUID()}@test.com", role = role)).id
    }!!.also { ownerIds.add(it) }

    private fun deposit(userId: Long, amount: Long = 100L): Long = tx.execute {
        depositRepository.save(ContributionDepositRequest.request(userId, amount)).id
    }!!

    private fun withdrawal(userId: Long, amount: Long): Long = tx.execute {
        withdrawalRepository.save(ContributionWithdrawalRequest.request(userId, amount)).id
    }!!

    @AfterEach
    fun removeCommittedFixtures() {
        tx.executeWithoutResult {
            ownerIds.forEach { userId ->
                ledger.deleteAll(ledger.findAllByUserIdOrderByOccurredAtDesc(userId))
                deposits.deleteAll(deposits.findAllByUserIdOrderByRequestedAtDesc(userId))
                withdrawals.deleteAll(withdrawals.findAllByUserIdOrderByRequestedAtDesc(userId))
                accounts.findById(userId).ifPresent(accounts::delete)
            }
            users.deleteAllById(ownerIds)
        }
        ownerIds.clear()
    }

    private fun race(prepare: () -> Unit, vararg actions: () -> Unit): List<Exception?> {
        val ready = CountDownLatch(actions.size)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(actions.size)
        try {
            val futures = actions.map { action ->
                pool.submit<Exception?> {
                    try {
                        // Include commit in the outcome, not just the use case return value.
                        tx.executeWithoutResult {
                            // Read pending requests before either worker writes, also establishing
                            // MySQL REPEATABLE READ snapshots before the competing commits.
                            prepare()
                            ready.countDown()
                            check(start.await(10, TimeUnit.SECONDS))
                            action()
                        }
                        null
                    } catch (failure: Exception) {
                        failure
                    }
                }
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS), "Workers did not enter their transactions")
            start.countDown()
            return futures.map { it.get(30, TimeUnit.SECONDS) }
        } finally {
            start.countDown()
            pool.shutdownNow()
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "Workers did not finish")
        }
    }

    companion object {
        private val h2Url = "jdbc:h2:mem:contribution-races-${UUID.randomUUID()};DB_CLOSE_DELAY=-1"

        @JvmStatic
        @DynamicPropertySource
        fun datasource(registry: DynamicPropertyRegistry) {
            val externalUrl = System.getenv("CONTRIBUTION_TEST_DATASOURCE_URL")
            registry.add("spring.datasource.url") { externalUrl ?: h2Url }
            registry.add("spring.datasource.username") {
                if (externalUrl == null) "sa" else requireNotNull(System.getenv("CONTRIBUTION_TEST_DATASOURCE_USERNAME"))
            }
            registry.add("spring.datasource.password") {
                if (externalUrl == null) "" else requireNotNull(System.getenv("CONTRIBUTION_TEST_DATASOURCE_PASSWORD"))
            }
            registry.add("spring.datasource.driver-class-name") {
                if (externalUrl == null) "org.h2.Driver" else "com.mysql.cj.jdbc.Driver"
            }
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
        }
    }

    @RepeatedTest(20)
    fun `simultaneous first deposits serialize account creation and preserve ledger`() {
        val userId = owner()
        val ids = listOf(deposit(userId, 3_000_000_000L), deposit(userId, 4_000_000_000L))
        val outcomes = race(
            { ids.forEach { depositRepository.findById(it)!! } },
            { approveDeposit.approve(ids[0], 99L) },
            { approveDeposit.approve(ids[1], 88L) },
        )
        assertEquals(listOf(null, null), outcomes)
        tx.executeWithoutResult {
            assertEquals(7_000_000_000L, accountRepository.findByUserId(userId)!!.balance)
            val entries = ledgerRepository.findAllByUserId(userId).sortedBy { it.beforeBalance }
            assertEquals(2, entries.size)
            assertEquals(ids.toSet(), entries.map { it.sourceRequestId }.toSet())
            assertTrue(entries.all { it.type == ContributionTransactionType.DEPOSIT })
            assertEquals(0L, entries.first().beforeBalance)
            assertEquals(entries.first().afterBalance, entries.last().beforeBalance)
            assertEquals(7_000_000_000L, entries.last().afterBalance)
            ids.zip(listOf(99L, 88L)).forEach { (id, processor) ->
                val request = depositRepository.findById(id)!!
                assertEquals(ContributionDepositRequestStatus.APPROVED, request.status)
                assertEquals(processor, request.processedBy)
                assertNotNull(request.processedAt)
                assertNull(request.rejectionReason)
            }
            assertEquals(Role.ROLE_MEMBER, users.findById(userId).orElseThrow().role)
        }
    }

    @RepeatedTest(20)
    fun `simultaneous approval of one request posts exactly one ledger entry`() {
        val userId = owner()
        val id = deposit(userId)
        val processors = listOf(99L, 88L)
        val outcomes = race(
            { depositRepository.findById(id)!! },
            { approveDeposit.approve(id, processors[0]) },
            { approveDeposit.approve(id, processors[1]) },
        )
        assertEquals(1, outcomes.count { it == null })
        val conflict = outcomes.filterNotNull().single()
        assertTrue(conflict is OptimisticLockingFailureException || conflict is ContributionDepositRequestAlreadyProcessedException)
        tx.executeWithoutResult {
            assertEquals(100L, accountRepository.findByUserId(userId)!!.balance)
            val entry = ledgerRepository.findAllByUserId(userId).single()
            assertEquals(id, entry.sourceRequestId)
            assertEquals(ContributionTransactionType.DEPOSIT, entry.type)
            assertEquals(100L, entry.amount)
            assertEquals(0L, entry.beforeBalance)
            assertEquals(100L, entry.afterBalance)
            val request = depositRepository.findById(id)!!
            assertEquals(ContributionDepositRequestStatus.APPROVED, request.status)
            assertEquals(processors[outcomes.indexOf(null)], request.processedBy)
            assertNotNull(request.processedAt)
            assertNull(request.rejectionReason)
            assertEquals(Role.ROLE_MEMBER, users.findById(userId).orElseThrow().role)
        }
    }

    @RepeatedTest(20)
    fun `concurrent withdrawals cannot overdraw the account`() {
        val userId = owner()
        val fundingId = deposit(userId)
        tx.executeWithoutResult { approveDeposit.approve(fundingId, 77L) }
        val ids = listOf(withdrawal(userId, 80L), withdrawal(userId, 80L))
        val processors = listOf(99L, 88L)
        val outcomes = race(
            { ids.forEach { withdrawalRepository.findById(it)!! } },
            { approveWithdrawal.approve(ids[0], processors[0]) },
            { approveWithdrawal.approve(ids[1], processors[1]) },
        )
        assertEquals(1, outcomes.count { it == null })
        assertTrue(outcomes.filterNotNull().single() is InsufficientContributionBalanceException)
        val winner = outcomes.indexOf(null)
        tx.executeWithoutResult {
            assertEquals(20L, accountRepository.findByUserId(userId)!!.balance)
            val entries = ledgerRepository.findAllByUserId(userId)
            assertEquals(2, entries.size)
            val funding = entries.single { it.type == ContributionTransactionType.DEPOSIT }
            assertEquals(fundingId, funding.sourceRequestId)
            assertEquals(0L, funding.beforeBalance)
            assertEquals(100L, funding.afterBalance)
            val paid = entries.single { it.type == ContributionTransactionType.WITHDRAWAL }
            assertEquals(ids[winner], paid.sourceRequestId)
            assertEquals(80L, paid.amount)
            assertEquals(100L, paid.beforeBalance)
            assertEquals(20L, paid.afterBalance)
            ids.forEachIndexed { index, id ->
                val request = withdrawalRepository.findById(id)!!
                if (index == winner) {
                    assertEquals(ContributionWithdrawalRequestStatus.APPROVED, request.status)
                    assertEquals(processors[index], request.processedBy)
                    assertNotNull(request.processedAt)
                } else {
                    assertEquals(ContributionWithdrawalRequestStatus.PENDING, request.status)
                    assertNull(request.processedBy)
                    assertNull(request.processedAt)
                }
                assertNull(request.rejectionReason)
            }
            assertEquals(Role.ROLE_MEMBER, users.findById(userId).orElseThrow().role)
        }
    }

    @RepeatedTest(20)
    fun `concurrent deposit approval and rejection have one exclusive winner`() {
        val userId = owner()
        val id = deposit(userId)
        val reject = RejectContributionDepositRequestUseCase(depositRepository)
        val outcomes = race(
            { depositRepository.findById(id)!! },
            { approveDeposit.approve(id, 99L) },
            { reject.reject(id, 88L, RejectContributionRequest("race rejection")) },
        )
        assertEquals(1, outcomes.count { it == null })
        val conflict = outcomes.filterNotNull().single()
        assertTrue(conflict is OptimisticLockingFailureException || conflict is ContributionDepositRequestAlreadyProcessedException)
        val approved = outcomes[0] == null
        tx.executeWithoutResult {
            val request = depositRepository.findById(id)!!
            assertNotNull(request.processedAt)
            val entries = ledgerRepository.findAllByUserId(userId)
            if (approved) {
                assertEquals(ContributionDepositRequestStatus.APPROVED, request.status)
                assertEquals(99L, request.processedBy)
                assertNull(request.rejectionReason)
                assertEquals(100L, accountRepository.findByUserId(userId)!!.balance)
                val entry = entries.single()
                assertEquals(id, entry.sourceRequestId)
                assertEquals(ContributionTransactionType.DEPOSIT, entry.type)
                assertEquals(100L, entry.amount)
                assertEquals(0L, entry.beforeBalance)
                assertEquals(100L, entry.afterBalance)
                assertEquals(Role.ROLE_MEMBER, users.findById(userId).orElseThrow().role)
            } else {
                assertEquals(ContributionDepositRequestStatus.REJECTED, request.status)
                assertEquals(88L, request.processedBy)
                assertEquals("race rejection", request.rejectionReason)
                assertNull(accountRepository.findByUserId(userId))
                assertTrue(entries.isEmpty())
                assertEquals(Role.ROLE_USER, users.findById(userId).orElseThrow().role)
            }
        }
    }

    @RepeatedTest(20)
    fun `concurrent withdrawal approval and rejection have one exclusive winner`() {
        val userId = owner()
        val fundingId = deposit(userId)
        tx.executeWithoutResult { approveDeposit.approve(fundingId, 77L) }
        val id = withdrawal(userId, 80L)
        val reject = RejectContributionWithdrawalRequestUseCase(withdrawalRepository)
        val outcomes = race(
            { withdrawalRepository.findById(id)!! },
            { approveWithdrawal.approve(id, 99L) },
            { reject.reject(id, 88L, RejectContributionRequest("race rejection")) },
        )
        assertEquals(1, outcomes.count { it == null })
        val conflict = outcomes.filterNotNull().single()
        assertTrue(conflict is OptimisticLockingFailureException || conflict is ContributionWithdrawalRequestAlreadyProcessedException)
        val approved = outcomes[0] == null
        tx.executeWithoutResult {
            val request = withdrawalRepository.findById(id)!!
            assertNotNull(request.processedAt)
            assertEquals(if (approved) 20L else 100L, accountRepository.findByUserId(userId)!!.balance)
            val entries = ledgerRepository.findAllByUserId(userId)
            val funding = entries.single { it.type == ContributionTransactionType.DEPOSIT }
            assertEquals(fundingId, funding.sourceRequestId)
            assertEquals(0L, funding.beforeBalance)
            assertEquals(100L, funding.afterBalance)
            if (approved) {
                assertEquals(ContributionWithdrawalRequestStatus.APPROVED, request.status)
                assertEquals(99L, request.processedBy)
                assertNull(request.rejectionReason)
                assertEquals(2, entries.size)
                val entry = entries.single { it.type == ContributionTransactionType.WITHDRAWAL }
                assertEquals(id, entry.sourceRequestId)
                assertEquals(80L, entry.amount)
                assertEquals(100L, entry.beforeBalance)
                assertEquals(20L, entry.afterBalance)
            } else {
                assertEquals(ContributionWithdrawalRequestStatus.REJECTED, request.status)
                assertEquals(88L, request.processedBy)
                assertEquals("race rejection", request.rejectionReason)
                assertEquals(1, entries.size)
            }
            assertEquals(Role.ROLE_MEMBER, users.findById(userId).orElseThrow().role)
        }
    }

    @Test
    fun `duplicate approvals and rejections cannot change balance or audit`() {
        val userId = owner(Role.ROLE_ADMIN)
        val id = deposit(userId)
        val approved = tx.execute { approveDeposit.approve(id, 99L) }!!
        assertEquals(99L, approved.processedBy)
        assertNotNull(approved.processedAt)
        val reloaded = depositRepository.findById(id)!!
        assertEquals(99L, reloaded.processedBy)
        assertEquals(approved.processedAt, reloaded.processedAt)
        assertThrows(ContributionDepositRequestAlreadyProcessedException::class.java) {
            tx.execute { approveDeposit.approve(id, 88L) }
        }
        assertThrows(ContributionDepositRequestAlreadyProcessedException::class.java) {
            RejectContributionDepositRequestUseCase(depositRepository).reject(id, 88L, null)
        }
        assertEquals(100L, accountRepository.findByUserId(userId)!!.balance)
        assertEquals(1, ledgerRepository.findAllByUserId(userId).size)
        assertEquals(Role.ROLE_ADMIN, users.findById(userId).orElseThrow().role)
    }

    @Test
    fun `withdrawal approval records audit and processed check precedes insufficient funds`() {
        val userId = owner()
        tx.execute { approveDeposit.approve(deposit(userId), 99L) }
        val id = withdrawal(userId, 100L)
        val response = tx.execute { approveWithdrawal.approve(id, 88L) }!!
        assertEquals(88L, response.processedBy)
        assertNotNull(response.processedAt)
        assertEquals(response.processedAt, withdrawalRepository.findById(id)!!.processedAt)
        assertEquals(0L, accountRepository.findByUserId(userId)!!.balance)
        assertThrows(ContributionWithdrawalRequestAlreadyProcessedException::class.java) {
            tx.execute { approveWithdrawal.approve(id, 77L) }
        }
        assertThrows(ContributionWithdrawalRequestAlreadyProcessedException::class.java) {
            RejectContributionWithdrawalRequestUseCase(withdrawalRepository).reject(id, 77L, null)
        }
        assertEquals(2, ledgerRepository.findAllByUserId(userId).size)
    }

    @Test
    fun `missing account withdrawal and overflow leave requests pending without ledger`() {
        val userId = owner()
        val id = withdrawal(userId, 1L)
        assertThrows(InsufficientContributionBalanceException::class.java) {
            tx.execute { approveWithdrawal.approve(id, 99L) }
        }
        assertNull(accountRepository.findByUserId(userId))
        assertEquals(ContributionWithdrawalRequestStatus.PENDING, withdrawalRepository.findById(id)!!.status)
        tx.execute { accountRepository.save(ContributionAccount(userId, Long.MAX_VALUE)) }
        val depositId = deposit(userId, 1L)
        assertThrows(ContributionBalanceOverflowException::class.java) {
            tx.execute { approveDeposit.approve(depositId, 99L) }
        }
        assertEquals(Long.MAX_VALUE, accountRepository.findByUserId(userId)!!.balance)
        assertEquals(ContributionDepositRequestStatus.PENDING, depositRepository.findById(depositId)!!.status)
        assertTrue(ledgerRepository.findAllByUserId(userId).isEmpty())
    }

    @Test
    fun `ledger failure rolls back account request and role`() {
        val userId = owner()
        val id = deposit(userId)
        val failingLedger = object : ContributionTransactionRepository by ledgerRepository {
            override fun save(transaction: ContributionTransaction): ContributionTransaction {
                ledgerRepository.save(transaction)
                throw IllegalStateException("ledger failure")
            }
        }
        val useCase = ApproveContributionDepositRequestUseCase(depositRepository, accountRepository, failingLedger)
        assertThrows(IllegalStateException::class.java) { tx.execute { useCase.approve(id, 99L) } }
        assertNull(accountRepository.findByUserId(userId))
        assertEquals(ContributionDepositRequestStatus.PENDING, depositRepository.findById(id)!!.status)
        assertTrue(ledgerRepository.findAllByUserId(userId).isEmpty())
        assertEquals(Role.ROLE_USER, users.findById(userId).orElseThrow().role)
    }

    @Test
    fun `withdrawal ledger failure restores balance and pending request`() {
        val userId = owner()
        tx.execute { approveDeposit.approve(deposit(userId), 99L) }
        val id = withdrawal(userId, 100L)
        val failingLedger = object : ContributionTransactionRepository by ledgerRepository {
            override fun save(transaction: ContributionTransaction): ContributionTransaction {
                ledgerRepository.save(transaction)
                throw IllegalStateException("ledger failure")
            }
        }
        val useCase = ApproveContributionWithdrawalRequestUseCase(withdrawalRepository, accountRepository, failingLedger)
        assertThrows(IllegalStateException::class.java) { tx.execute { useCase.approve(id, 88L) } }
        assertEquals(100L, accountRepository.findByUserId(userId)!!.balance)
        val pending = withdrawalRepository.findById(id)!!
        assertEquals(ContributionWithdrawalRequestStatus.PENDING, pending.status)
        assertNull(pending.processedBy)
        assertNull(pending.processedAt)
        assertEquals(listOf(ContributionTransactionType.DEPOSIT), ledgerRepository.findAllByUserId(userId).map { it.type })
    }

    @Test
    fun `stale account and request versions fail instead of losing updates`() {
        val userId = owner()
        val id = deposit(userId)
        val staleRequest = depositRepository.findById(id)!!
        tx.execute { approveDeposit.approve(id, 99L) }
        assertThrows(OptimisticLockingFailureException::class.java) {
            tx.execute { deposits.saveAndFlush(ContributionDepositRequestPersistenceMapper.toEntity(staleRequest.reject(88L))) }
        }
        val staleAccount = accountRepository.findByUserId(userId)!!
        tx.execute { accounts.saveAndFlush(ContributionAccountPersistenceMapper.toEntity(staleAccount.deposit(1L))) }
        assertThrows(OptimisticLockingFailureException::class.java) {
            tx.execute { accounts.saveAndFlush(ContributionAccountPersistenceMapper.toEntity(staleAccount.deposit(2L))) }
        }
        assertEquals(101L, accountRepository.findByUserId(userId)!!.balance)
        val withdrawalId = withdrawal(userId, 1L)
        val staleWithdrawal = withdrawalRepository.findById(withdrawalId)!!
        tx.execute { approveWithdrawal.approve(withdrawalId, 99L) }
        assertThrows(OptimisticLockingFailureException::class.java) {
            tx.execute {
                withdrawals.saveAndFlush(ContributionWithdrawalRequestPersistenceMapper.toEntity(staleWithdrawal.reject(88L)))
            }
        }
        assertEquals(100L, accountRepository.findByUserId(userId)!!.balance)
    }

    @Test
    fun `rejection audit roundtrips and blocks later approval`() {
        val userId = owner()
        val depositId = deposit(userId)
        val withdrawalId = withdrawal(userId, 1L)
        RejectContributionDepositRequestUseCase(depositRepository).reject(depositId, 77L, RejectContributionRequest("reason"))
        RejectContributionWithdrawalRequestUseCase(withdrawalRepository).reject(withdrawalId, 88L, RejectContributionRequest("reason"))
        val rejectedDeposit = depositRepository.findById(depositId)!!
        val rejectedWithdrawal = withdrawalRepository.findById(withdrawalId)!!
        assertEquals(77L, rejectedDeposit.processedBy)
        assertEquals(88L, rejectedWithdrawal.processedBy)
        assertNotNull(rejectedDeposit.processedAt)
        assertNotNull(rejectedWithdrawal.processedAt)
        assertEquals("reason", rejectedDeposit.rejectionReason)
        assertEquals("reason", rejectedWithdrawal.rejectionReason)
        assertThrows(ContributionDepositRequestAlreadyProcessedException::class.java) {
            tx.execute { approveDeposit.approve(depositId, 99L) }
        }
        assertThrows(ContributionWithdrawalRequestAlreadyProcessedException::class.java) {
            tx.execute { approveWithdrawal.approve(withdrawalId, 99L) }
        }
    }
}
