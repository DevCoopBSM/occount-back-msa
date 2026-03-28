package devcoop.occount.payment.application.shared

data class ChargeRequest(
    val amount: Int,
    val method: String
)
