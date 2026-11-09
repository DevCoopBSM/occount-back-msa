package devcoop.occount.member.application.query

import devcoop.occount.member.application.output.ContributionWithdrawalRequestRepository
import devcoop.occount.member.application.usecase.contribution.ContributionWithdrawalRequestResponse
import org.springframework.stereotype.Service

@Service
class GetMyContributionWithdrawalRequestsQueryService(
    private val contributionWithdrawalRequestRepository: ContributionWithdrawalRequestRepository,
) {
    fun findAll(userId: Long): ContributionWithdrawalRequestListResponse {
        return ContributionWithdrawalRequestListResponse(
            requests = contributionWithdrawalRequestRepository.findAllByUserId(userId)
                .map(ContributionWithdrawalRequestResponse::from),
        )
    }
}
