package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.application.output.ContributionWithdrawalRequestRepository
import devcoop.occount.member.application.output.PageResult
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequest
import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequestStatus
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository

@Repository
class ContributionWithdrawalRequestRepositoryImpl(
    private val contributionWithdrawalRequestPersistenceRepository: ContributionWithdrawalRequestPersistenceRepository,
) : ContributionWithdrawalRequestRepository {
    override fun findById(id: Long): ContributionWithdrawalRequest? {
        return contributionWithdrawalRequestPersistenceRepository.findById(id)
            .map(ContributionWithdrawalRequestPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun save(request: ContributionWithdrawalRequest): ContributionWithdrawalRequest {
        return contributionWithdrawalRequestPersistenceRepository
            .save(ContributionWithdrawalRequestPersistenceMapper.toEntity(request))
            .let(ContributionWithdrawalRequestPersistenceMapper::toDomain)
    }

    override fun findPage(
        status: ContributionWithdrawalRequestStatus?,
        page: Int,
        size: Int,
    ): PageResult<ContributionWithdrawalRequest> {
        val pageable = PageRequest.of(page, size)
        val result = status
            ?.let { contributionWithdrawalRequestPersistenceRepository.findAllByStatusOrderByRequestedAtDesc(it, pageable) }
            ?: contributionWithdrawalRequestPersistenceRepository.findAllByOrderByRequestedAtDesc(pageable)

        return PageResult(
            content = result.content.map(ContributionWithdrawalRequestPersistenceMapper::toDomain),
            page = result.number,
            size = result.size,
            totalCount = result.totalElements.toInt(),
            totalPages = result.totalPages,
        )
    }

    override fun findAllByUserId(userId: Long): List<ContributionWithdrawalRequest> {
        return contributionWithdrawalRequestPersistenceRepository.findAllByUserIdOrderByRequestedAtDesc(userId)
            .map(ContributionWithdrawalRequestPersistenceMapper::toDomain)
    }
}
