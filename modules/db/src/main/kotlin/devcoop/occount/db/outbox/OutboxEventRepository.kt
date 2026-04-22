package devcoop.occount.db.outbox

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
interface OutboxEventRepository : JpaRepository<OutboxEventJpaEntity, String> {
    fun findTop100ByPublishedFalseOrderByOccurredAtAsc(): List<OutboxEventJpaEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(
        """
        update OutboxEventJpaEntity event
        set event.published = true, event.publishedAt = :publishedAt
        where event.eventId = :eventId and event.published = false
        """,
    )
    fun markPublished(
        @Param("eventId") eventId: String,
        @Param("publishedAt") publishedAt: Instant,
    ): Int
}
