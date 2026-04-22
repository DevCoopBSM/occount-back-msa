package devcoop.occount.order.application.shared

data class OrderStreamEvent(
    val type: OrderStreamEventType,
    val payload: OrderResponse,
)
