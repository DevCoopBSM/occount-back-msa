package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.exception.ContributionDepositRequestNotFoundException
import devcoop.occount.member.application.support.FakeContributionAccountRepository
import devcoop.occount.member.application.support.FakeContributionDepositRequestRepository
import devcoop.occount.member.application.support.FakeContributionTransactionRepository
import devcoop.occount.member.domain.contribution.ContributionAccount
import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import devcoop.occount.member.domain.contribution.ContributionDepositRequestAlreadyProcessedException
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ApproveContributionDepositRequestUseCase 단위 테스트")
class ApproveContributionDepositRequestUseCaseTest {
    @Test
    @DisplayName("출자금 납입 신청을 승인하고 회원 출자금 잔액을 증가시킨다")
    fun `approve approves request and increases contribution balance`() {
        val requestRepository = FakeContributionDepositRequestRepository(
            listOf(ContributionDepositRequest.request(userId = 1L, amount = 10000)),
        )
        val accountRepository = FakeContributionAccountRepository(
            listOf(ContributionAccount(userId = 1L, balance = 5000)),
        )
        val transactionRepository = FakeContributionTransactionRepository()
        val useCase = ApproveContributionDepositRequestUseCase(requestRepository, accountRepository, transactionRepository)

        val response = useCase.approve(requestId = 1L)

        assertEquals(ContributionDepositRequestStatus.APPROVED, response.status)
        assertEquals(15000, accountRepository.findByUserId(1L)?.balance)
        assertEquals(15000, transactionRepository.saved.single().afterBalance)
    }

    @Test
    @DisplayName("출자금 계좌가 없으면 승인 시 새 계좌를 만든다")
    fun `approve creates account when account does not exist`() {
        val requestRepository = FakeContributionDepositRequestRepository(
            listOf(ContributionDepositRequest.request(userId = 1L, amount = 10000)),
        )
        val accountRepository = FakeContributionAccountRepository()
        val useCase = ApproveContributionDepositRequestUseCase(
            requestRepository,
            accountRepository,
            FakeContributionTransactionRepository(),
        )

        useCase.approve(requestId = 1L)

        assertEquals(10000, accountRepository.findByUserId(1L)?.balance)
    }

    @Test
    @DisplayName("존재하지 않는 출자금 납입 신청은 승인할 수 없다")
    fun `approve fails when request does not exist`() {
        val useCase = ApproveContributionDepositRequestUseCase(
            FakeContributionDepositRequestRepository(),
            FakeContributionAccountRepository(),
            FakeContributionTransactionRepository(),
        )

        assertFailsWith<ContributionDepositRequestNotFoundException> {
            useCase.approve(requestId = 999L)
        }
    }

    @Test
    @DisplayName("이미 처리된 출자금 납입 신청은 승인할 수 없다")
    fun `approve fails when request already processed`() {
        val requestRepository = FakeContributionDepositRequestRepository(
            listOf(ContributionDepositRequest.request(userId = 1L, amount = 10000).approve()),
        )
        val useCase = ApproveContributionDepositRequestUseCase(
            requestRepository,
            FakeContributionAccountRepository(),
            FakeContributionTransactionRepository(),
        )

        assertFailsWith<ContributionDepositRequestAlreadyProcessedException> {
            useCase.approve(requestId = 1L)
        }
    }
}
