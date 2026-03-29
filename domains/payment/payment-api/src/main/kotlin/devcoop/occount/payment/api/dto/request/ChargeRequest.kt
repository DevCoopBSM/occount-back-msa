package devcoop.occount.payment.api.dto.request

import jakarta.validation.constraints.Positive

data class ChargeRequest(
    @param:Positive(message = "충전 금액은 1원 이상이어야 합니다.")
    val amount: Int
)
