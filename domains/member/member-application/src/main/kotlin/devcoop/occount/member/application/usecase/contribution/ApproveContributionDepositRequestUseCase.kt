package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.exception.ContributionDepositRequestNotFoundException
import devcoop.occount.member.application.output.ContributionAccountRepository
import devcoop.occount.member.application.output.ContributionDepositRequestRepository
import devcoop.occount.member.application.output.ContributionTransactionRepository
import devcoop.occount.member.domain.contribution.ContributionAccount
import devcoop.occount.member.domain.contribution.ContributionTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ApproveContributionDepositRequestUseCase(
    private val contributionDepositRequestRepository: ContributionDepositRequestRepository,
    private val contributionAccountRepository: ContributionAccountRepository,
    private val contributionTransactionRepository: ContributionTransactionRepository,
) {
    @Transactional
    fun approve(requestId: Long, processedBy: Long): ContributionDepositRequestResponse {
        val request = contributionDepositRequestRepository.findById(requestId)
            ?: throw ContributionDepositRequestNotFoundException()

        val processedRequest = request.approve(processedBy)
        val account = contributionAccountRepository.findByUserIdForUpdate(request.userId)
            ?: ContributionAccount(userId = request.userId)
        val updatedAccount = contributionAccountRepository.save(account.deposit(request.amount))
        contributionTransactionRepository.save(
            ContributionTransaction.deposit(
                userId = request.userId,
                amount = request.amount,
                beforeBalance = account.balance,
                afterBalance = updatedAccount.balance,
                sourceRequestId = request.id,
            ),
        )

        contributionAccountRepository.promoteOwnerToMember(request.userId)
        val approvedRequest = contributionDepositRequestRepository.save(processedRequest)
        return ContributionDepositRequestResponse.from(approvedRequest)
    }
}
