package devcoop.occount.member.application.support

import devcoop.occount.member.application.output.ContributionDepositRequestRepository
import devcoop.occount.member.application.output.PageResult
import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus

class FakeContributionDepositRequestRepository(
    initialRequests: List<ContributionDepositRequest> = emptyList(),
) : ContributionDepositRequestRepository {
    private val requestsById = linkedMapOf<Long, ContributionDepositRequest>()
    private var nextId = 1L

    init {
        initialRequests.forEach(::save)
    }

    override fun findById(id: Long): ContributionDepositRequest? = requestsById[id]

    override fun save(request: ContributionDepositRequest): ContributionDepositRequest {
        val persistedRequest = if (request.id == 0L) request.copy(id = nextId++) else request
        requestsById[persistedRequest.id] = persistedRequest
        return persistedRequest
    }

    override fun findPage(
        status: ContributionDepositRequestStatus?,
        page: Int,
        size: Int,
    ): PageResult<ContributionDepositRequest> {
        val requests = requestsById.values
            .filter { status == null || it.status == status }
            .sortedByDescending { it.requestedAt }
        return PageResult(
            content = requests.drop(page * size).take(size),
            page = page,
            size = size,
            totalCount = requests.size,
            totalPages = if (requests.isEmpty()) 0 else (requests.size + size - 1) / size,
        )
    }

    override fun findAllByUserId(userId: Long): List<ContributionDepositRequest> {
        return requestsById.values
            .filter { it.userId == userId }
            .sortedByDescending { it.requestedAt }
    }
}
