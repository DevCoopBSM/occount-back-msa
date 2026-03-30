package devcoop.occount.payment.application.shared

data class PaymentItem(
    val itemId: String,
    val itemName: String,
    val itemPrice: Int,
    val quantity: Int,
    val totalPrice: Int
)
