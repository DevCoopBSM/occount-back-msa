package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.output.ContributionWithdrawalRequestRepository
import devcoop.occount.member.application.output.PageResult
import devcoop.occount.member.application.support.FakeContributionAccountRepository
import devcoop.occount.member.application.support.FakeContributionTransactionRepository
import devcoop.occount.member.domain.contribution.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.jupiter.api.Test

class ApproveContributionWithdrawalRequestUseCaseTest {
    private class Requests(var request: ContributionWithdrawalRequest) : ContributionWithdrawalRequestRepository {
        override fun findById(id: Long) = request.takeIf { it.id == id }
        override fun save(request: ContributionWithdrawalRequest): ContributionWithdrawalRequest {
            this.request = request
            return request
        }
        override fun findAllByUserId(userId: Long) = listOf(request).filter { it.userId == userId }
        override fun findPage(status: ContributionWithdrawalRequestStatus?, page: Int, size: Int): PageResult<ContributionWithdrawalRequest> =
            throw UnsupportedOperationException("not used by processing")
    }

    @Test
    fun `approval writes exact Long ledger transition and cannot be processed twice`() {
        val requests = Requests(ContributionWithdrawalRequest.request(1L, 3_000_000_000L).copy(id = 1L))
        val accounts = FakeContributionAccountRepository(listOf(ContributionAccount(1L, 3_000_000_000L)))
        val ledger = FakeContributionTransactionRepository()
        val useCase = ApproveContributionWithdrawalRequestUseCase(requests, accounts, ledger)
        val response = useCase.approve(1L, 99L)
        assertEquals(99L, response.processedBy)
        assertNotNull(response.processedAt)
        assertEquals(0L, accounts.findByUserId(1L)!!.balance)
        assertEquals(3_000_000_000L, ledger.saved.single().beforeBalance)
        assertEquals(0L, ledger.saved.single().afterBalance)
        assertFailsWith<ContributionWithdrawalRequestAlreadyProcessedException> { useCase.approve(1L, 88L) }
        assertFailsWith<ContributionWithdrawalRequestAlreadyProcessedException> {
            RejectContributionWithdrawalRequestUseCase(requests).reject(1L, 88L)
        }
        assertEquals(1, ledger.saved.size)
    }

    @Test
    fun `missing account fails insufficient funds without saving account or processing request`() {
        val requests = Requests(ContributionWithdrawalRequest.request(1L, 1L).copy(id = 1L))
        val accounts = FakeContributionAccountRepository()
        val ledger = FakeContributionTransactionRepository()
        val useCase = ApproveContributionWithdrawalRequestUseCase(requests, accounts, ledger)
        assertFailsWith<InsufficientContributionBalanceException> { useCase.approve(1L, 99L) }
        assertNull(accounts.findByUserId(1L))
        assertEquals(ContributionWithdrawalRequestStatus.PENDING, requests.request.status)
        assertEquals(0, ledger.saved.size)
    }

    @Test
    fun `rejection keeps balances untouched and rejects duplicate processing`() {
        val requests = Requests(ContributionWithdrawalRequest.request(1L, 1L).copy(id = 1L))
        val response = RejectContributionWithdrawalRequestUseCase(requests).reject(1L, 88L, RejectContributionRequest("reason"))
        assertEquals(ContributionWithdrawalRequestStatus.REJECTED, response.status)
        assertEquals("reason", response.rejectionReason)
        assertEquals(88L, response.processedBy)
        assertNotNull(response.processedAt)
        assertFailsWith<ContributionWithdrawalRequestAlreadyProcessedException> {
            RejectContributionWithdrawalRequestUseCase(requests).reject(1L, 99L)
        }
    }
}
