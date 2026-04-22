package devcoop.occount.suggestion.infrastructure.persistence.aripick

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AripickLikePersistenceRepository : JpaRepository<AripickLikeJpaEntity, Long> {
    fun existsByProposalIdAndUserId(proposalId: Long, userId: Long): Boolean

    @Modifying
    @Query(
        value = "INSERT IGNORE INTO aripick_like (proposal_id, user_id) VALUES (:proposalId, :userId)",
        nativeQuery = true,
    )
    fun saveLikeIfAbsent(
        @Param("proposalId") proposalId: Long,
        @Param("userId") userId: Long,
    ): Int

    @Modifying
    @Query("DELETE FROM AripickLikeJpaEntity l WHERE l.proposalId = :proposalId AND l.userId = :userId")
    fun deleteByProposalIdAndUserId(
        @Param("proposalId") proposalId: Long,
        @Param("userId") userId: Long,
    ): Int

    @Modifying
    @Query("DELETE FROM AripickLikeJpaEntity l WHERE l.proposalId = :proposalId")
    fun deleteByProposalId(@Param("proposalId") proposalId: Long): Int
}
