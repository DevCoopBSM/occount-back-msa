package devcoop.occount.point.application.usecase.charge

data class ChargePointRequest(
    val userId: Long,
    val amount: Int,
)
