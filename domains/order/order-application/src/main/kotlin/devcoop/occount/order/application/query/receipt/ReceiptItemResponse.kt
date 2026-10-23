package devcoop.occount.order.application.query.receipt

data class ReceiptItemResponse(
    val itemId: Long,
    val itemName: String,
    val unitPrice: Int,
    val quantity: Int,
    val totalPrice: Int,
)
