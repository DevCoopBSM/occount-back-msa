package devcoop.occount.payment.infrastructure.member

data class UserPaymentInfoReadResponse(
    val userId: Long,
    val email: String,
)
