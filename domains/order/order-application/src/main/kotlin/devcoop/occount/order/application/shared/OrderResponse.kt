package devcoop.occount.order.application.shared

import devcoop.occount.order.domain.order.OrderStatus

data class OrderResponse(
    val orderId: Long,
    val status: OrderStatus,
    val failureReason: String? = null,
)
