package devcoop.occount.db.outbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OutboxEventRepository : JpaRepository<OutboxEventJpaEntity, String> {
    fun findTop100ByPublishedFalseOrderByOccurredAtAsc(): List<OutboxEventJpaEntity>
}
