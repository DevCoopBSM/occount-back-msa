package devcoop.occount.order.application.shared

data class OrderItemRequest(
    val itemId: Long,
    val itemName: String,
    val itemPrice: Int,
    val quantity: Int,
)
