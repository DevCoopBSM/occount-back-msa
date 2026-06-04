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
                .content("""{"amount": 10000, "depositorName": "홍길동", "bankName": "국민은행"}"""),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.amount").value(10000))
            .andExpect(jsonPath("$.depositorName").value("홍길동"))
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
        fixture.depositRepository.save(ContributionDepositRequest.request(userId = 2L, amount = 20000).approve())

        fixture.mockMvc.perform(
            get("/contributions/deposit-requests")
                .param("status", "PENDING")
                .param("page", "0")
                .param("size", "10"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.requests.length()").value(1))
            .andExpect(jsonPath("$.requests[0].status").value("PENDING"))
            .andExpect(jsonPath("$.page.totalCount").value(1))
    }

    @Test
    @DisplayName("관리자가 출자금 납입 신청을 승인한다")
    fun `approveDepositRequest approves request`() {
        val fixture = Fixture()
        val savedRequest = fixture.depositRepository.save(
            ContributionDepositRequest.request(userId = 1L, amount = 10000),
        )

        fixture.mockMvc.perform(
            patch("/contributions/deposit-requests/${savedRequest.id}/approve"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPROVED"))
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
                .content("""{"amount": 5000, "bankName": "국민은행", "accountNumber": "123"}"""),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.amount").value(5000))
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    @DisplayName("관리자가 상태별 출자금 반환 신청 목록을 조회한다")
    fun `getWithdrawalRequests filters by status`() {
        val fixture = Fixture()
        fixture.withdrawalRepository.save(ContributionWithdrawalRequest.request(userId = 1L, amount = 5000))
        fixture.withdrawalRepository.save(ContributionWithdrawalRequest.request(userId = 2L, amount = 7000).reject())

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
            patch("/contributions/withdrawal-requests/${savedRequest.id}/approve"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("APPROVED"))
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

        override fun findById(id: Long): ContributionDepositRequest? = requestsById[id]

        override fun save(request: ContributionDepositRequest): ContributionDepositRequest {
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
    ) : ContributionAccountRepository {
        private val accountsByUserId = linkedMapOf<Long, ContributionAccount>().apply {
            initialAccounts.forEach { account -> put(account.userId, account) }
        }

        override fun findByUserId(userId: Long): ContributionAccount? = accountsByUserId[userId]

        override fun save(account: ContributionAccount): ContributionAccount {
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
