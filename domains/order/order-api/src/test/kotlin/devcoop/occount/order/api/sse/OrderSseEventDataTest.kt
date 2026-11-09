package devcoop.occount.order.api.sse

import devcoop.occount.order.application.shared.OrderStreamEvent
import devcoop.occount.order.application.shared.OrderStreamEventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("OrderSse data 페이로드 SNAKE_CASE 계약")
class OrderSseEventDataTest {

    @Test
    @DisplayName("정상 이벤트 data 키는 order_id 하나이며 snake_case다")
    fun `normal event exposes only order_id`() {
        val data = DefaultOrderSseEmitterSupport.eventData(
            OrderStreamEvent(type = OrderStreamEventType.COMPLETED, orderId = 1001L),
        )

        assertEquals(1001L, data["order_id"])
        assertFalse(data.containsKey("failure_reason"))
        // camelCase 키가 새어나가지 않아야 한다 (Map 키는 naming strategy를 타지 않으므로 회귀 방지)
        assertFalse(data.containsKey("orderId"))
    }

    @Test
    @DisplayName("실패 이벤트 data는 order_id + failure_reason(snake_case)을 포함한다")
    fun `failed event exposes failure_reason in snake_case`() {
        val data = DefaultOrderSseEmitterSupport.eventData(
            OrderStreamEvent(type = OrderStreamEventType.FAILED, orderId = 1001L, failureReason = "재고 부족"),
        )

        assertEquals(1001L, data["order_id"])
        assertEquals("재고 부족", data["failure_reason"])
        assertFalse(data.containsKey("failureReason"))
    }
}
