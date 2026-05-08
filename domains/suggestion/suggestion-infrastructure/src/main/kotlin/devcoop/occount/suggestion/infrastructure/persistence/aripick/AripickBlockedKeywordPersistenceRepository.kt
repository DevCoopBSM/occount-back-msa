package devcoop.occount.suggestion.infrastructure.persistence.aripick

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AripickBlockedKeywordPersistenceRepository : JpaRepository<AripickBlockedKeywordJpaEntity, Long> {
    @Query(
        "SELECT CASE WHEN COUNT(k) > 0 THEN true ELSE false END " +
            "FROM AripickBlockedKeywordJpaEntity k " +
            "WHERE LOWER(:name) LIKE CONCAT('%', LOWER(k.keyword), '%')",
    )
    fun hasBlockedKeyword(@Param("name") name: String): Boolean

    fun existsByKeywordIgnoreCase(keyword: String): Boolean

    fun findAllByOrderByKeywordIdDesc(): List<AripickBlockedKeywordJpaEntity>
}
