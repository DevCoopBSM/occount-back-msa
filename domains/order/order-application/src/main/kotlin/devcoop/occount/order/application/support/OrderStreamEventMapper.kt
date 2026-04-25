package devcoop.occount.order.application.support

import devcoop.occount.order.application.shared.OrderStreamEvent
import devcoop.occount.order.application.shared.OrderStreamEventType
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.OrderStepStatus
import org.springframework.stereotype.Component

@Component
class OrderStreamEventMapper {
    fun toStreamEvent(order: OrderAggregate): OrderStreamEvent {
        return OrderStreamEvent(
            type = resolveType(order),
            orderId = order.orderId,
        )
    }

    private fun resolveType(order: OrderAggregate): OrderStreamEventType {
        return when (order.status) {
            OrderStatus.PENDING, OrderStatus.PROCESSING -> resolveProcessingType(order)
            OrderStatus.COMPLETED -> OrderStreamEventType.COMPLETED
            OrderStatus.FAILED -> OrderStreamEventType.FAILED
            OrderStatus.CANCEL_REQUESTED -> OrderStreamEventType.CANCEL_REQUESTED
            OrderStatus.COMPENSATING -> {
                if (order.cancelRequested) {
                    OrderStreamEventType.CANCEL_REQUESTED
                } else {
                    OrderStreamEventType.FAILED
                }
            }
            OrderStatus.CANCELLED -> OrderStreamEventType.CANCELLED
            OrderStatus.COMPENSATION_FAILED -> OrderStreamEventType.FAILED
            OrderStatus.TIMED_OUT -> OrderStreamEventType.TIMED_OUT
        }
    }

    private fun resolveProcessingType(order: OrderAggregate): OrderStreamEventType {
        return when {
            order.paymentRequested && order.paymentStatus == OrderStepStatus.PENDING ->
                OrderStreamEventType.PAYMENT_REQUESTED
            else -> OrderStreamEventType.ORDER_ACCEPTED
        }
    }
}
