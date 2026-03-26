package devcoop.occount.point.application.usecase.deduct

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class DeductPointRequest(
    @field:NotNull
    val userId: Long,

    @field:Positive
    val amount: Int,
)
