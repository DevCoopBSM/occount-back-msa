package devcoop.occount.db.outbox

import devcoop.occount.core.common.event.EventPublisher
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

@Component
class OutboxEventPublisher(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) : EventPublisher {
    override fun publish(topic: String, key: String, eventType: String, payload: Any) {
        outboxEventRepository.save(
            OutboxEventJpaEntity(
                eventId = UUID.randomUUID().toString(),
                topic = topic,
                messageKey = key,
                eventType = eventType,
                payload = objectMapper.writeValueAsString(payload),
                occurredAt = Instant.now(),
            ),
        )
    }
}
