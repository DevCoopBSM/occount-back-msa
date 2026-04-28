package devcoop.occount.suggestion.infrastructure.persistence.aripick

import devcoop.occount.suggestion.application.output.AripickPolicyRepository
import devcoop.occount.suggestion.domain.aripick.AripickBlockedKeyword
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Repository

@Repository
class AripickPolicyRepositoryImpl(
    private val blockedKeywordPersistenceRepository: AripickBlockedKeywordPersistenceRepository,
) : AripickPolicyRepository {
    override fun hasBlockedKeyword(name: String): Boolean {
        return blockedKeywordPersistenceRepository.hasBlockedKeyword(name.trim())
    }

    override fun existsBlockedKeyword(keyword: String): Boolean {
        return blockedKeywordPersistenceRepository.existsByKeywordIgnoreCase(keyword.trim())
    }

    override fun findBlockedKeywords(): List<AripickBlockedKeyword> {
        return blockedKeywordPersistenceRepository.findAllByOrderByKeywordIdDesc()
            .map(::toDomain)
    }

    override fun saveBlockedKeyword(keyword: String): AripickBlockedKeyword {
        val normalized = keyword.trim()
        val saved = blockedKeywordPersistenceRepository.save(
            AripickBlockedKeywordJpaEntity(keyword = normalized),
        )
        return toDomain(saved)
    }

    override fun deleteBlockedKeyword(keywordId: Long) {
        try {
            blockedKeywordPersistenceRepository.deleteById(keywordId)
        } catch (_: EmptyResultDataAccessException) {
            return
        }
    }

    private fun toDomain(entity: AripickBlockedKeywordJpaEntity): AripickBlockedKeyword {
        return AripickBlockedKeyword(
            keywordId = entity.getKeywordId(),
            keyword = entity.getKeyword(),
            registeredDate = entity.getRegisteredDate(),
        )
    }
}
