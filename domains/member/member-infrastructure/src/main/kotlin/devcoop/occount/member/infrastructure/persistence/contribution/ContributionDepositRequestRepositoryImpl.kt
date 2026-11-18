package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.application.output.ContributionDepositRequestRepository
import devcoop.occount.member.application.output.PageResult
import devcoop.occount.member.domain.contribution.ContributionDepositRequest
import devcoop.occount.member.domain.contribution.ContributionDepositRequestStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class ContributionDepositRequestRepositoryImpl(
    private val contributionDepositRequestPersistenceRepository: ContributionDepositRequestPersistenceRepository,
) : ContributionDepositRequestRepository {
    override fun findById(id: Long): ContributionDepositRequest? {
        return contributionDepositRequestPersistenceRepository.findById(id)
            .map(ContributionDepositRequestPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun save(request: ContributionDepositRequest): ContributionDepositRequest {
        return contributionDepositRequestPersistenceRepository
            .save(ContributionDepositRequestPersistenceMapper.toEntity(request))
            .let(ContributionDepositRequestPersistenceMapper::toDomain)
    }

    override fun findPage(
        status: ContributionDepositRequestStatus?,
        page: Int,
        size: Int,
    ): PageResult<ContributionDepositRequest> {
        val pageable = PageRequest.of(page, size)
        val result = status
            ?.let { contributionDepositRequestPersistenceRepository.findAllByStatusOrderByRequestedAtDesc(it, pageable) }
            ?: contributionDepositRequestPersistenceRepository.findAllByOrderByRequestedAtDesc(pageable)

        return PageResult(
            content = result.content.map(ContributionDepositRequestPersistenceMapper::toDomain),
            page = result.number,
            size = result.size,
            totalCount = result.totalElements.toInt(),
            totalPages = result.totalPages,
        )
    }

    override fun findAllByUserId(userId: Long): List<ContributionDepositRequest> {
        return contributionDepositRequestPersistenceRepository.findAllByUserIdOrderByRequestedAtDesc(userId)
            .map(ContributionDepositRequestPersistenceMapper::toDomain)
    }
}
