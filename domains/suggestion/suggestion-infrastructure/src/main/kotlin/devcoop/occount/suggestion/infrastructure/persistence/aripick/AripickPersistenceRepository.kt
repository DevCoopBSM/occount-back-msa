package devcoop.occount.suggestion.infrastructure.persistence.aripick

import devcoop.occount.suggestion.domain.aripick.AripickStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AripickPersistenceRepository : JpaRepository<AripickJpaEntity, Long> {
    fun countByStatus(status: AripickStatus): Long

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AripickJpaEntity p SET p.status = :status WHERE p.proposalId = :proposalId")
    fun updateStatus(
        @Param("proposalId") proposalId: Long,
        @Param("status") status: AripickStatus,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AripickJpaEntity p SET p.likeCount = p.likeCount + 1 WHERE p.proposalId = :proposalId")
    fun increaseLikeCount(@Param("proposalId") proposalId: Long): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AripickJpaEntity p SET p.likeCount = p.likeCount - 1 WHERE p.proposalId = :proposalId AND p.likeCount > 0")
    fun decreaseLikeCount(@Param("proposalId") proposalId: Long): Int
}
