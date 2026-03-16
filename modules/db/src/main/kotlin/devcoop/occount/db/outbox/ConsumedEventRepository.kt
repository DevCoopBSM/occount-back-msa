package devcoop.occount.db.outbox

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ConsumedEventRepository : JpaRepository<ConsumedEventJpaEntity, String>
