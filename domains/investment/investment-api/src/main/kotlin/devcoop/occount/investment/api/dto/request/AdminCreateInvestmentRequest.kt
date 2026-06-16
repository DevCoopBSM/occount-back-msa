package devcoop.occount.investment.api.dto.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDateTime

data class AdminCreateInvestmentRequest(
    @field:Positive
    val userId: Long = 0L,
    @field:Positive
    @field:Max(10_000_000)
    val amount: Int = 0,
    @field:Pattern(regexp = "DEPOSIT|DONATION|RETURNED")
    val type: String = "DEPOSIT",
    val depositDate: LocalDateTime? = null,
    val conversionDate: LocalDateTime? = null,
    @field:Size(max = 255)
    val confirmMethod: String? = null,
)
