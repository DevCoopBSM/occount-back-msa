package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.exception.ContributionWithdrawalRequestNotFoundException
import devcoop.occount.member.application.output.ContributionAccountRepository
import devcoop.occount.member.application.output.ContributionTransactionRepository
import devcoop.occount.member.application.output.ContributionWithdrawalRequestRepository
import devcoop.occount.member.domain.contribution.ContributionAccount
import devcoop.occount.member.domain.contribution.ContributionTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ApproveContributionWithdrawalRequestUseCase(
    private val contributionWithdrawalRequestRepository: ContributionWithdrawalRequestRepository,
    private val contributionAccountRepository: ContributionAccountRepository,
    private val contributionTransactionRepository: ContributionTransactionRepository,
) {
    @Transactional
    fun approve(requestId: Long): ContributionWithdrawalRequestResponse {
        val request = contributionWithdrawalRequestRepository.findById(requestId)
            ?: throw ContributionWithdrawalRequestNotFoundException()

        val account = contributionAccountRepository.findByUserId(request.userId)
            ?: ContributionAccount(userId = request.userId)
        val updatedAccount = account.withdraw(request.amount)
        val approvedRequest = contributionWithdrawalRequestRepository.save(request.approve())
        val savedAccount = contributionAccountRepository.save(updatedAccount)
        contributionTransactionRepository.save(
            ContributionTransaction.withdrawal(
                userId = request.userId,
                amount = request.amount,
                beforeBalance = account.balance,
                afterBalance = savedAccount.balance,
                sourceRequestId = request.id,
            ),
        )

        return ContributionWithdrawalRequestResponse.from(approvedRequest)
    }
}
