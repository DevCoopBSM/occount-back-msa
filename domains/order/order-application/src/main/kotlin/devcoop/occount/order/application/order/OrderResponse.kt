package devcoop.occount.order.application.order

import devcoop.occount.order.domain.order.OrderStatus

data class OrderResponse(
    val orderId: String,
    val status: OrderStatus,
    val failureReason: String? = null,
)
