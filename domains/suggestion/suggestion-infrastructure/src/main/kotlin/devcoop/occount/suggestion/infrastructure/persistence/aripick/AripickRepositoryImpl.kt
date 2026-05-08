package devcoop.occount.suggestion.infrastructure.persistence.aripick

import devcoop.occount.suggestion.application.output.AripickRepository
import devcoop.occount.suggestion.domain.aripick.AripickItem
import devcoop.occount.suggestion.domain.aripick.AripickStatus
import org.springframework.stereotype.Repository

@Repository
class AripickRepositoryImpl(
    private val aripickPersistenceRepository: AripickPersistenceRepository,
    private val aripickLikePersistenceRepository: AripickLikePersistenceRepository,
) : AripickRepository {
    override fun findAll(): List<AripickItem> {
        return aripickPersistenceRepository.findAll()
            .map(AripickPersistenceMapper::toDomain)
    }

    override fun findById(proposalId: Long): AripickItem? {
        return aripickPersistenceRepository.findById(proposalId)
            .map(AripickPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun save(aripickItem: AripickItem): AripickItem {
        return aripickPersistenceRepository.save(AripickPersistenceMapper.toEntity(aripickItem))
            .let(AripickPersistenceMapper::toDomain)
    }

    override fun updateStatus(proposalId: Long, status: AripickStatus): Boolean {
        return aripickPersistenceRepository.updateStatus(proposalId, status) > 0
    }

    override fun deleteById(proposalId: Long) {
        aripickPersistenceRepository.deleteById(proposalId)
    }

    override fun existsLike(proposalId: Long, userId: Long): Boolean {
        return aripickLikePersistenceRepository.existsByProposalIdAndUserId(proposalId, userId)
    }

    override fun saveLikeIfAbsent(proposalId: Long, userId: Long): Boolean {
        return aripickLikePersistenceRepository.saveLikeIfAbsent(proposalId, userId) > 0
    }

    override fun deleteLike(proposalId: Long, userId: Long): Boolean {
        return aripickLikePersistenceRepository.deleteByProposalIdAndUserId(proposalId, userId) > 0
    }

    override fun increaseLikeCount(proposalId: Long): Boolean {
        return aripickPersistenceRepository.increaseLikeCount(proposalId) > 0
    }

    override fun decreaseLikeCount(proposalId: Long): Boolean {
        return aripickPersistenceRepository.decreaseLikeCount(proposalId) > 0
    }

    override fun deleteLikesByProposalId(proposalId: Long) {
        aripickLikePersistenceRepository.deleteByProposalId(proposalId)
    }

    override fun countAll(): Long {
        return aripickPersistenceRepository.count()
    }

    override fun countByStatus(status: AripickStatus): Long {
        return aripickPersistenceRepository.countByStatus(status)
    }
}
