package devcoop.occount.member.application.usecase.contribution

import devcoop.occount.member.application.exception.ContributionDepositRequestNotFoundException
import devcoop.occount.member.application.output.ContributionDepositRequestRepository
import org.springframework.stereotype.Service

@Service
class RejectContributionDepositRequestUseCase(
    private val contributionDepositRequestRepository: ContributionDepositRequestRepository,
) {
    fun reject(requestId: Long, command: RejectContributionRequest = RejectContributionRequest()): ContributionDepositRequestResponse {
        val depositRequest = contributionDepositRequestRepository.findById(requestId)
            ?: throw ContributionDepositRequestNotFoundException()

        return contributionDepositRequestRepository.save(depositRequest.reject(command.reason))
            .let(ContributionDepositRequestResponse::from)
    }
}
