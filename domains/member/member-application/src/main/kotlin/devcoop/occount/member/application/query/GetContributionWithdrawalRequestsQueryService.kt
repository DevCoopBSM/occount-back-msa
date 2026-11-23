package devcoop.occount.member.application.query

import devcoop.occount.member.application.output.ContributionWithdrawalRequestRepository
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequestStatus
import org.springframework.stereotype.Service

@Service
class GetContributionWithdrawalRequestsQueryService(
    private val contributionWithdrawalRequestRepository: ContributionWithdrawalRequestRepository,
    private val userRepository: UserRepository,
) {
    fun findAll(
        status: ContributionWithdrawalRequestStatus?,
        page: Int,
        size: Int,
    ): AdminContributionWithdrawalRequestListResponse {
        val requestPage = contributionWithdrawalRequestRepository.findPage(
            status = status,
            page = page.coerceAtLeast(0),
            size = size.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE),
        )
        val usersById = userRepository.findAllByIds(requestPage.content.map { it.userId }.distinct())
            .associateBy { it.getId() }

        return AdminContributionWithdrawalRequestListResponse(
            requests = requestPage.content.map { request ->
                AdminContributionWithdrawalRequestResponse.from(
                    request = request,
                    user = usersById[request.userId],
                )
            },
            page = PageMeta(
                page = requestPage.page,
                size = requestPage.size,
                totalCount = requestPage.totalCount,
                totalPages = requestPage.totalPages,
            ),
        )
    }

    private companion object {
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 100
    }
}
