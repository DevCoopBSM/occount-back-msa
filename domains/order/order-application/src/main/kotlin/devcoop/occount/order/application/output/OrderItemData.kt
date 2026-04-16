package devcoop.occount.order.application.output

data class OrderItemData(
    val itemId: Long,
    val itemName: String,
    val itemPrice: Int,
    val isActive: Boolean,
)
