package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.exception.ContributionDepositRequestNotFoundException
import devcoop.occount.member.application.output.ContributionDepositRequestRepository
import org.springframework.stereotype.Service

@Service
class RejectContributionDepositRequestUseCase(
    private val contributionDepositRequestRepository: ContributionDepositRequestRepository,
) {
    fun reject(requestId: Long, processedBy: Long, request: RejectContributionRequest? = null): ContributionDepositRequestResponse {
        val depositRequest = contributionDepositRequestRepository.findById(requestId)
            ?: throw ContributionDepositRequestNotFoundException()

        return contributionDepositRequestRepository.save(depositRequest.reject(processedBy, request?.reason))
            .let(ContributionDepositRequestResponse::from)
    }
}
