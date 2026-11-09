package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.output.ContributionWithdrawalRequestRepository
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequest
import org.springframework.stereotype.Service

@Service
class SubmitContributionWithdrawalRequestUseCase(
    private val contributionWithdrawalRequestRepository: ContributionWithdrawalRequestRepository,
) {
    fun submit(userId: Long, request: SubmitContributionWithdrawalRequest): ContributionWithdrawalRequestResponse {
        val withdrawalRequest = ContributionWithdrawalRequest.request(
            userId = userId,
            amount = request.amount,
            bankName = request.bankName,
            accountNumber = request.accountNumber,
            accountHolder = request.accountHolder,
            memo = request.memo,
        )

        return contributionWithdrawalRequestRepository.save(withdrawalRequest)
            .let(ContributionWithdrawalRequestResponse::from)
    }
}
