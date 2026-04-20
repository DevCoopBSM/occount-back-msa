package devcoop.occount.order.application.shared

data class OrderItemRequest(
    val itemId: Long,
    val quantity: Int,
)
