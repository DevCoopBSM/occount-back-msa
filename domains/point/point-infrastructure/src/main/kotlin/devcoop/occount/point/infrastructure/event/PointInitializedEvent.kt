package devcoop.occount.point.infrastructure.event

data class PointInitializedEvent(
    val userId: Long,
    val balance: Int,
)
