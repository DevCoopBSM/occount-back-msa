package devcoop.occount.order.application.support

import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.domain.order.OrderAggregate
import org.springframework.stereotype.Component

@Component
class OrderResponseMapper {
    fun toResponse(order: OrderAggregate): OrderResponse {
        return OrderResponse(
            orderId = order.orderId,
            status = order.status,
            failureReason = order.failureReason,
        )
    }
}
