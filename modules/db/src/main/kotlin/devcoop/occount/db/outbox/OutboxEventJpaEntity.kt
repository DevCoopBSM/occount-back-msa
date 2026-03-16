package devcoop.occount.db.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "outbox_event")
class OutboxEventJpaEntity(
    @Id
    @field:Column(nullable = false, length = 36)
    private var eventId: String = "",
    @field:Column(nullable = false)
    private var topic: String = "",
    @field:Column(nullable = false)
    private var messageKey: String = "",
    @field:Column(nullable = false)
    private var eventType: String = "",
    @field:Column(nullable = false, columnDefinition = "TEXT")
    private var payload: String = "",
    @field:Column(nullable = false)
    private var occurredAt: Instant = Instant.now(),
    @field:Column(nullable = false)
    private var published: Boolean = false,
    @field:Column
    private var publishedAt: Instant? = null,
) {
    fun getEventId() = eventId
    fun getTopic() = topic
    fun getMessageKey() = messageKey
    fun getEventType() = eventType
    fun getPayload() = payload
    fun isPublished() = published

    fun markPublished(publishedAt: Instant) {
        this.published = true
        this.publishedAt = publishedAt
    }
}
