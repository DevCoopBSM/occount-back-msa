package devcoop.occount.db.outbox

import devcoop.occount.core.common.event.EventPublisher
import io.micrometer.tracing.Tracer
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Component
class OutboxEventPublisher(
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
    private val tracer: Optional<Tracer>,
) : EventPublisher {
    override fun publish(topic: String, key: String, eventType: String, payload: Any) {
        val traceparent = tracer.orElse(null)?.currentSpan()?.context()?.let { ctx ->
            val flags = if (ctx.sampled() == true) "01" else "00"
            "00-${ctx.traceId()}-${ctx.spanId()}-$flags"
        }
        outboxEventRepository.save(
            OutboxEventJpaEntity(
                eventId = UUID.randomUUID().toString(),
                topic = topic,
                messageKey = key,
                eventType = eventType,
                payload = objectMapper.writeValueAsString(payload),
                occurredAt = Instant.now(),
                traceId = traceparent,
            ),
        )
    }
}
