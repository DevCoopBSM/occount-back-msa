package devcoop.occount.member.application.query

import devcoop.occount.member.application.output.ContributionDepositRequestRepository
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus
import org.springframework.stereotype.Service

@Service
class GetContributionDepositRequestsQueryService(
    private val contributionDepositRequestRepository: ContributionDepositRequestRepository,
    private val userRepository: UserRepository,
) {
    fun findAll(
        status: ContributionDepositRequestStatus?,
        page: Int,
        size: Int,
    ): AdminContributionDepositRequestListResponse {
        val requestPage = contributionDepositRequestRepository.findPage(
            status = status,
            page = page.coerceAtLeast(0),
            size = size.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE),
        )
        val usersById = userRepository.findAllByIds(requestPage.content.map { it.userId }.distinct())
            .associateBy { it.getId() }

        return AdminContributionDepositRequestListResponse(
            requests = requestPage.content.map { request ->
                AdminContributionDepositRequestResponse.from(
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
