package devcoop.occount.suggestion.infrastructure.persistence.aripick

import devcoop.occount.suggestion.application.output.AripickPolicyRepository
import devcoop.occount.suggestion.domain.aripick.AripickBlockedKeyword
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
        return blockedKeywordPersistenceRepository.findAll()
            .sortedByDescending { it.getKeywordId() }
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
        if (!blockedKeywordPersistenceRepository.existsById(keywordId)) {
            return
        }
        blockedKeywordPersistenceRepository.deleteById(keywordId)
    }

    private fun toDomain(entity: AripickBlockedKeywordJpaEntity): AripickBlockedKeyword {
        return AripickBlockedKeyword(
            keywordId = entity.getKeywordId(),
            keyword = entity.getKeyword(),
            registeredDate = entity.getRegisteredDate(),
        )
    }
}
