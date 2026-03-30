package devcoop.occount.payment.application.shared

data class PointBalanceChange(
    val beforeBalance: Int,
    val changedAmount: Int,
    val afterBalance: Int,
)
