package devcoop.occount.order.domain.order

data class OrderLine(
    val itemId: Long,
    val itemNameSnapshot: String,
    val unitPrice: Int,
    val quantity: Int,
    val totalPrice: Int,
)
