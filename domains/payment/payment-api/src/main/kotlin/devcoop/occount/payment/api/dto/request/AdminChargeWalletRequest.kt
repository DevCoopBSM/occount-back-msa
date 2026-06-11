package devcoop.occount.payment.api.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class AdminChargeWalletRequest(
    @field:NotEmpty
    @field:Size(max = 100)
    val userIds: List<Long> = emptyList(),
    @field:Positive
    @field:Max(10_000_000)
    val amount: Int = 0,
    @field:NotBlank
    @field:Size(max = 255)
    val reason: String = "",
)
