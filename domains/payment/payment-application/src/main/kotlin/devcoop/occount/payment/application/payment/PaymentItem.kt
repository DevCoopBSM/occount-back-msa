package devcoop.occount.payment.application.payment

data class PaymentItem(
    val itemId: String,
    val itemName: String,
    val itemPrice: Int,
    val quantity: Int,
    val totalPrice: Int
)
