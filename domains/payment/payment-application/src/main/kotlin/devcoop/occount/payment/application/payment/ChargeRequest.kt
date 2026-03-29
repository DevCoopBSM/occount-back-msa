package devcoop.occount.payment.application.payment

data class ChargeRequest(
    val amount: Int,
    val method: String
)
