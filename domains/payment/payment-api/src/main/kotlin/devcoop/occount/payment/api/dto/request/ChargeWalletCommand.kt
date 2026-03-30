package devcoop.occount.payment.api.dto.request

import jakarta.validation.constraints.Positive

data class ChargeWalletCommand(
    @field:Positive
    val amount: Int,
)
