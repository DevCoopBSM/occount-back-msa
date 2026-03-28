package devcoop.occount.payment.application.shared

data class PaymentDetails(
    val items: List<PaymentItem>,
    val totalAmount: Int
)
