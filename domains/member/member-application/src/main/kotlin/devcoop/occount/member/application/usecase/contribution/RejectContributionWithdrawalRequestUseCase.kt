package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.exception.ContributionWithdrawalRequestNotFoundException
import devcoop.occount.member.application.output.ContributionWithdrawalRequestRepository
import org.springframework.stereotype.Service

@Service
class RejectContributionWithdrawalRequestUseCase(
    private val contributionWithdrawalRequestRepository: ContributionWithdrawalRequestRepository,
) {
    fun reject(requestId: Long, command: RejectContributionRequest = RejectContributionRequest()): ContributionWithdrawalRequestResponse {
        val withdrawalRequest = contributionWithdrawalRequestRepository.findById(requestId)
            ?: throw ContributionWithdrawalRequestNotFoundException()

        return contributionWithdrawalRequestRepository.save(withdrawalRequest.reject(command.reason))
            .let(ContributionWithdrawalRequestResponse::from)
    }
}
