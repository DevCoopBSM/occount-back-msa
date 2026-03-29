package devcoop.occount.payment.application.payment

data class PointBalanceChange(
    val beforeBalance: Int,
    val changedAmount: Int,
    val afterBalance: Int,
)
