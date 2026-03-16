package devcoop.occount.point.application.point

import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.point.domain.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PointServiceTest {
    @Test
    fun `initialize creates point and publishes initialized event`() {
        val repository = FakePointRepository()
        val eventPublisher = RecordingEventPublisher()
        val service = PointService(repository, eventPublisher)

        val point = service.initialize(1L)

        assertEquals(0, point.balance)
        assertEquals(1, repository.savedPoints.size)
        assertEquals(1, eventPublisher.events.size)
        assertEquals(DomainTopics.POINT_INITIALIZED, eventPublisher.events.single().topic)
        assertEquals("PointInitializedEvent", eventPublisher.events.single().eventType)
        val payload = eventPublisher.events.single().payload as Map<*, *>
        assertEquals(1L, payload["userId"])
        assertEquals(0, payload["balance"])
    }

    @Test
    fun `initialize returns existing point without publishing event`() {
        val repository = FakePointRepository(
            points = mutableMapOf(1L to Point(userId = 1L, balance = 50)),
        )
        val eventPublisher = RecordingEventPublisher()
        val service = PointService(repository, eventPublisher)

        val point = service.initialize(1L)

        assertEquals(50, point.balance)
        assertTrue(repository.savedPoints.isEmpty())
        assertTrue(eventPublisher.events.isEmpty())
    }

    @Test
    fun `charge saves updated point and publishes balance changed event`() {
        val repository = FakePointRepository()
        val eventPublisher = RecordingEventPublisher()
        val service = PointService(repository, eventPublisher)

        val response = service.charge(1L, 70)

        assertEquals(70, response.balance)
        assertEquals(1, repository.savedPoints.size)
        assertEquals(DomainTopics.POINT_BALANCE_CHANGED, eventPublisher.events.single().topic)
        assertEquals("PointBalanceChangedEvent", eventPublisher.events.single().eventType)
        val payload = eventPublisher.events.single().payload as Map<*, *>
        assertEquals(70, payload["balance"])
        assertEquals(70, payload["changedAmount"])
    }

    @Test
    fun `deduct publishes negative changed amount`() {
        val repository = FakePointRepository(
            points = mutableMapOf(1L to Point(userId = 1L, balance = 100)),
        )
        val eventPublisher = RecordingEventPublisher()
        val service = PointService(repository, eventPublisher)

        val response = service.deduct(1L, 25)

        assertEquals(75, response.balance)
        val payload = eventPublisher.events.single().payload as Map<*, *>
        assertEquals(-25, payload["changedAmount"])
    }

    private class FakePointRepository(
        private val points: MutableMap<Long, Point> = mutableMapOf(),
    ) : PointRepository {
        val savedPoints = mutableListOf<Point>()

        override fun findByUserId(userId: Long): Point? = points[userId]

        override fun save(point: Point): Point {
            points[point.userId] = point
            savedPoints += point
            return point
        }
    }

    private class RecordingEventPublisher : EventPublisher {
        val events = mutableListOf<PublishedEvent>()

        override fun publish(topic: String, key: String, eventType: String, payload: Any) {
            events += PublishedEvent(topic, key, eventType, payload)
        }
    }

    private data class PublishedEvent(
        val topic: String,
        val key: String,
        val eventType: String,
        val payload: Any,
    )
}
