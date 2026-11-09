package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.exception.ContributionWithdrawalRequestNotFoundException
import devcoop.occount.member.application.output.ContributionWithdrawalRequestRepository
import org.springframework.stereotype.Service

@Service
class RejectContributionWithdrawalRequestUseCase(
    private val contributionWithdrawalRequestRepository: ContributionWithdrawalRequestRepository,
) {
    fun reject(requestId: Long, processedBy: Long, request: RejectContributionRequest? = null): ContributionWithdrawalRequestResponse {
        val withdrawalRequest = contributionWithdrawalRequestRepository.findById(requestId)
            ?: throw ContributionWithdrawalRequestNotFoundException()

        return contributionWithdrawalRequestRepository.save(withdrawalRequest.reject(processedBy, request?.reason))
            .let(ContributionWithdrawalRequestResponse::from)
    }
}
