package devcoop.occount.payment.application.payment

data class PaymentDetails(
    val items: List<PaymentItem>,
    val totalAmount: Int
)
