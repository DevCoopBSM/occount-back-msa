package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.exception.ContributionWithdrawalRequestNotFoundException
import devcoop.occount.member.application.output.ContributionAccountRepository
import devcoop.occount.member.application.output.ContributionTransactionRepository
import devcoop.occount.member.application.output.ContributionWithdrawalRequestRepository
import devcoop.occount.member.domain.contribution.InsufficientContributionBalanceException
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
    fun approve(requestId: Long, processedBy: Long): ContributionWithdrawalRequestResponse {
        val request = contributionWithdrawalRequestRepository.findById(requestId)
            ?: throw ContributionWithdrawalRequestNotFoundException()

        val processedRequest = request.approve(processedBy)
        val account = contributionAccountRepository.findByUserIdForUpdate(request.userId)
            ?: throw InsufficientContributionBalanceException()
        val updatedAccount = account.withdraw(request.amount)
        val approvedRequest = contributionWithdrawalRequestRepository.save(processedRequest)
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
