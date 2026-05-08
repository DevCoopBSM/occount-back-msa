package devcoop.occount.kafka.outbox

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.db.outbox.OutboxEventRepository
import io.micrometer.tracing.CurrentTraceContext
import io.micrometer.tracing.Tracer

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.nio.charset.StandardCharsets

@Component
class OutboxRelay(
    private val outboxEventRepository: OutboxEventRepository,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val tracer: Tracer,
) {
    @Scheduled(fixedDelay = 50)
    fun relay() {
        outboxEventRepository.findTop100ByPublishedFalseOrderByOccurredAtAsc()
            .forEach { event ->
                val producerRecord = ProducerRecord(
                    event.getTopic(),
                    event.getMessageKey(),
                    event.getPayload(),
                ).apply {
                    headers().add(
                        RecordHeader(
                            DomainEventHeaders.EVENT_ID,
                            event.getEventId().toByteArray(StandardCharsets.UTF_8),
                        ),
                    )
                    headers().add(
                        RecordHeader(
                            DomainEventHeaders.EVENT_TYPE,
                            event.getEventType().toByteArray(StandardCharsets.UTF_8),
                        ),
                    )
                }

                val scope = restoreTraceContext(event.getTraceId())
                try {
                    kafkaTemplate.send(producerRecord).get()
                    outboxEventRepository.markPublished(event.getEventId(), Instant.now())
                } catch (ex: Exception) {
                    log.warn("Failed to relay outbox event. eventId={}", event.getEventId(), ex)
                    throw ex
                } finally {
                    scope?.close()
                }
            }
    }

    private fun restoreTraceContext(traceparent: String?): CurrentTraceContext.Scope? {
        if (traceparent == null) return null
        val parts = traceparent.split("-")
        if (parts.size < 4) return null

        val traceContext = tracer.traceContextBuilder()
            .traceId(parts[1])
            .spanId(parts[2])
            .sampled(parts[3] == "01")
            .build()

        return tracer.currentTraceContext().newScope(traceContext)
    }

    companion object {
        private val log = LoggerFactory.getLogger(OutboxRelay::class.java)
    }
}
