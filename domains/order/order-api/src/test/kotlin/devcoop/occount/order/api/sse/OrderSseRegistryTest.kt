package devcoop.occount.order.api.sse

import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.shared.OrderStreamEvent
import devcoop.occount.order.application.shared.OrderStreamEventType
import devcoop.occount.order.domain.order.OrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
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
            payload = OrderResponse(orderId = "order-1", status = OrderStatus.PROCESSING),
        )

        val emitter = registry.register("order-1") { initial } as TestSseEmitter

        registry.notify(initial)
        registry.notify(
            OrderStreamEvent(
                type = OrderStreamEventType.COMPLETED,
                payload = OrderResponse(orderId = "order-1", status = OrderStatus.COMPLETED),
            ),
        )

        assertEquals(
            listOf(
                initial,
                OrderStreamEvent(
                    type = OrderStreamEventType.COMPLETED,
                    payload = OrderResponse(orderId = "order-1", status = OrderStatus.COMPLETED),
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
            payload = OrderResponse(orderId = "order-1", status = OrderStatus.COMPLETED),
        )

        val emitter = registry.register("order-1") { completed } as TestSseEmitter

        assertEquals(listOf(completed), emitter.events)
        assertTrue(emitter.completed)
    }

    @Test
    fun `notify arriving between registration and initial emit is not lost`() {
        val support = TestOrderSseEmitterSupport()
        val registry = OrderSseRegistry(support)
        val staleEvent = OrderStreamEvent(
            type = OrderStreamEventType.ORDER_ACCEPTED,
            payload = OrderResponse(orderId = "order-1", status = OrderStatus.PROCESSING),
        )
        val completedEvent = OrderStreamEvent(
            type = OrderStreamEventType.COMPLETED,
            payload = OrderResponse(orderId = "order-1", status = OrderStatus.COMPLETED),
        )

        // getCurrentEvent() 실행 중 notify()가 먼저 도착하는 race condition 시뮬레이션
        val emitter = registry.register("order-1") {
            registry.notify(completedEvent)
            staleEvent  // 조회는 오래된 상태를 반환
        } as TestSseEmitter

        assertEquals(listOf(completedEvent), emitter.events)
        assertTrue(emitter.completed)
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
