package devcoop.occount.member.application.query

import devcoop.occount.member.application.output.ContributionDepositRequestRepository
import devcoop.occount.member.application.usecase.contribution.ContributionDepositRequestResponse
import org.springframework.stereotype.Service

@Service
class GetMyContributionDepositRequestsQueryService(
    private val contributionDepositRequestRepository: ContributionDepositRequestRepository,
) {
    fun findAll(userId: Long): ContributionDepositRequestListResponse {
        return ContributionDepositRequestListResponse(
            requests = contributionDepositRequestRepository.findAllByUserId(userId)
                .map(ContributionDepositRequestResponse::from),
        )
    }
}
