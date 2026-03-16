package devcoop.occount.point.infrastructure.event

data class PointBalanceChangedEvent(
    val userId: Long,
    val balance: Int,
    val changedAmount: Int,
)
