package devcoop.occount.point.api.dto.request

import jakarta.validation.constraints.Positive

data class ChargePointCommand(
    @field:Positive("충전 금액은 0 이상이어야 합니다.")
    val amount: Int,
)
