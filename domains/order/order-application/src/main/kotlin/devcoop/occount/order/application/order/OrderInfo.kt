package devcoop.occount.order.application.order

data class OrderInfo(
    val itemId: Long,
    val itemName: String,
    val itemPrice: Int,
    val orderQuantity: Int,
    val totalPrice: Int,
)
