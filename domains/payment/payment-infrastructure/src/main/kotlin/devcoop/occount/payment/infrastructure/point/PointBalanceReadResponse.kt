package devcoop.occount.payment.infrastructure.point

data class PointBalanceReadResponse(
    val userId: Long,
    val balance: Int,
)
