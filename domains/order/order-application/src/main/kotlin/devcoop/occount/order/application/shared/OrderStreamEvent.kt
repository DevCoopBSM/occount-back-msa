package devcoop.occount.order.application.shared

data class OrderStreamEvent(
    val type: OrderStreamEventType,
    val orderId: Long,
    val failureReason: String? = null,
)
