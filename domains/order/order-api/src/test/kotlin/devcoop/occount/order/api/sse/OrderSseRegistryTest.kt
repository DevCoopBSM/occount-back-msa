package devcoop.occount.order.api.sse

import devcoop.occount.order.application.shared.OrderStreamEvent
import devcoop.occount.order.application.shared.OrderStreamEventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

class OrderSseRegistryTest {
    @Test
    fun `register emits current event first and skips duplicate updates`() {
        val support = TestOrderSseEmitterSupport()
        val registry = OrderSseRegistry(support)
        val initial = OrderStreamEvent(
            type = OrderStreamEventType.ORDER_ACCEPTED,
            orderId = 1L,
        )

        val emitter = registry.register(1L) { initial } as TestSseEmitter

        registry.notify(initial)
        registry.notify(
            OrderStreamEvent(
                type = OrderStreamEventType.COMPLETED,
                orderId = 1L,
            ),
        )

        assertEquals(
            listOf(
                initial,
                OrderStreamEvent(
                    type = OrderStreamEventType.COMPLETED,
                    orderId = 1L,
                ),
            ),
            emitter.events,
        )
        assertTrue(emitter.completed)
    }

    @Test
    fun `register completes immediately for final status`() {
        val support = TestOrderSseEmitterSupport()
        val registry = OrderSseRegistry(support)
        val completed = OrderStreamEvent(
            type = OrderStreamEventType.COMPLETED,
            orderId = 1L,
        )

        val emitter = registry.register(1L) { completed } as TestSseEmitter

        assertEquals(listOf(completed), emitter.events)
        assertTrue(emitter.completed)
    }

    @Test
    fun `notify arriving between registration and initial emit is not lost`() {
        val support = TestOrderSseEmitterSupport()
        val registry = OrderSseRegistry(support)
        val staleEvent = OrderStreamEvent(
            type = OrderStreamEventType.ORDER_ACCEPTED,
            orderId = 1L,
        )
        val completedEvent = OrderStreamEvent(
            type = OrderStreamEventType.COMPLETED,
            orderId = 1L,
        )

        // getCurrentEvent() 실행 중 notify()가 먼저 도착하는 race condition 시뮬레이션
        val emitter = registry.register(1L) {
            registry.notify(completedEvent)
            staleEvent  // 조회는 오래된 상태를 반환
        } as TestSseEmitter

        assertEquals(listOf(completedEvent), emitter.events)
        assertTrue(emitter.completed)
    }

    @Test
    fun `notify with same frontend event type is skipped`() {
        val support = TestOrderSseEmitterSupport()
        val registry = OrderSseRegistry(support)
        val initial = OrderStreamEvent(
            type = OrderStreamEventType.CANCEL_REQUESTED,
            orderId = 1L,
        )
        val compensating = OrderStreamEvent(
            type = OrderStreamEventType.CANCEL_REQUESTED,
            orderId = 1L,
        )

        val emitter = registry.register(1L) { initial } as TestSseEmitter

        registry.notify(compensating)

        assertEquals(listOf(initial), emitter.events)
        assertFalse(emitter.completed)
    }

    private class TestOrderSseEmitterSupport : OrderSseEmitterSupport {
        override fun create(): SseEmitter = TestSseEmitter()

        override fun emit(emitter: SseEmitter, event: OrderStreamEvent) {
            (emitter as TestSseEmitter).events += event
        }
    }

    private class TestSseEmitter : SseEmitter(0L) {
        val events = mutableListOf<OrderStreamEvent>()
        var completed = false

        override fun complete() {
            completed = true
        }
    }
}
