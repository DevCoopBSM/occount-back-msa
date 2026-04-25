package devcoop.occount.kafka.outbox

import devcoop.occount.db.outbox.OutboxEventJpaEntity
import devcoop.occount.db.outbox.OutboxEventRepository
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import io.micrometer.tracing.Tracer
import org.mockito.Mockito.mock
import org.springframework.kafka.support.SendResult
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import java.time.Instant
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class OutboxRelayTest {
    @Test
    fun `marks event as published after kafka send succeeds`() {
        val event = outboxEvent()
        val outboxEventRepository = FakeOutboxEventRepository(listOf(event))
        val future = CompletableFuture.completedFuture(
            SendResult<String, String>(
                ProducerRecord(event.getTopic(), event.getMessageKey(), event.getPayload()),
                mock(RecordMetadata::class.java),
            ),
        )
        val kafkaTemplate = TestKafkaTemplate(future)

        OutboxRelay(outboxEventRepository.repository, kafkaTemplate, mock(Tracer::class.java)).relay()

        assertEquals(1, outboxEventRepository.findCalls)
        assertEquals(1, outboxEventRepository.publishedMarks.size)
        assertEquals(event.getEventId(), outboxEventRepository.publishedMarks.single().first)
    }

    @Test
    fun `does not mark event as published when kafka send fails`() {
        val event = outboxEvent()
        val outboxEventRepository = FakeOutboxEventRepository(listOf(event))
        val future = CompletableFuture<SendResult<String, String>>()
        val kafkaTemplate = TestKafkaTemplate(future)

        future.completeExceptionally(IllegalStateException("send failed"))

        assertThrows<ExecutionException> {
            OutboxRelay(outboxEventRepository.repository, kafkaTemplate, mock(Tracer::class.java)).relay()
        }
        assertEquals(1, outboxEventRepository.findCalls)
        assertEquals(0, outboxEventRepository.publishedMarks.size)
    }

    private class TestKafkaTemplate(
        private val future: CompletableFuture<SendResult<String, String>>,
    ) : KafkaTemplate<String, String>(mock(ProducerFactory::class.java) as ProducerFactory<String, String>) {
        override fun send(record: ProducerRecord<String, String>): CompletableFuture<SendResult<String, String>> {
            return future
        }
    }

    private class FakeOutboxEventRepository(
        private val events: List<OutboxEventJpaEntity>,
    ) : InvocationHandler {
        var findCalls: Int = 0
        val publishedMarks = mutableListOf<Pair<String, Instant>>()

        val repository: OutboxEventRepository = Proxy.newProxyInstance(
            OutboxEventRepository::class.java.classLoader,
            arrayOf(OutboxEventRepository::class.java),
            this,
        ) as OutboxEventRepository

        override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any?>?): Any? {
            return when (method.name) {
                "findTop100ByPublishedFalseOrderByOccurredAtAsc" -> {
                    findCalls += 1
                    events
                }

                "markPublished" -> {
                    publishedMarks += (args!![0] as String) to (args[1] as Instant)
                    1
                }

                "toString" -> "FakeOutboxEventRepository"
                "hashCode" -> System.identityHashCode(proxy)
                "equals" -> proxy === args?.firstOrNull()
                else -> throw UnsupportedOperationException("Unsupported method: ${method.name}")
            }
        }
    }

    private fun outboxEvent() = OutboxEventJpaEntity(
        eventId = "event-1",
        topic = "topic-1",
        messageKey = "key-1",
        eventType = "event-type",
        payload = """{"orderId":"order-1"}""",
        occurredAt = Instant.parse("2026-04-22T00:00:00Z"),
    )
}
