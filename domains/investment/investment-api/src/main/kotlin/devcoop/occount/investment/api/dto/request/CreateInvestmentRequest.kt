package devcoop.occount.investment.api.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive

data class CreateInvestmentRequest(
    @field:Positive
    @field:Max(10_000_000)
    val amount: Int = 0,
)
