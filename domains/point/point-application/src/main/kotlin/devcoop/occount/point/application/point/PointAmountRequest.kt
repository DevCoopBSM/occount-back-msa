package devcoop.occount.point.application.point

import jakarta.validation.constraints.Positive

data class PointAmountRequest(
    @field:Positive
    val amount: Int,
)
