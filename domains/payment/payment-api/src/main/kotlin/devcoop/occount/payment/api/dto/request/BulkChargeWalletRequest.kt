package devcoop.occount.payment.api.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive

data class BulkChargeWalletRequest(
    @field:NotEmpty(message = "충전 대상 사용자를 선택해주세요.")
    val userIds: List<Long>,
    @field:Positive(message = "충전 포인트는 0보다 커야 합니다.")
    val amount: Int,
    @field:NotBlank(message = "충전 사유를 입력해주세요.")
    val reason: String,
)
