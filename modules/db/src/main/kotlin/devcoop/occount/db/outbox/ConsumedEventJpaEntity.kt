package devcoop.occount.db.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "consumed_event")
class ConsumedEventJpaEntity(
    @Id
    @field:Column(nullable = false, length = 120)
    private var id: String = "",
    @field:Column(nullable = false, length = 60)
    private var consumerName: String = "",
    @field:Column(nullable = false, length = 36)
    private var eventId: String = "",
    @field:Column(nullable = false)
    private var processedAt: Instant = Instant.now(),
)
