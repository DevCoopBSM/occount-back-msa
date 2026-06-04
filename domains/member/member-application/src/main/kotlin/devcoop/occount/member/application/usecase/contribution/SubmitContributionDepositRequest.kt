package devcoop.occount.member.application.usecase.contribution

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class SubmitContributionDepositRequest(
    @field:Positive(message = "출자금 납입 신청 금액은 0보다 커야 합니다.")
    val amount: Int,
    @field:Size(max = 100, message = "입금자명은 100자 이하만 가능합니다.")
    val depositorName: String? = null,
    @field:Size(max = 100, message = "은행명은 100자 이하만 가능합니다.")
    val bankName: String? = null,
    @field:Size(max = 500, message = "증빙 이미지 URL은 500자 이하만 가능합니다.")
    val proofImageUrl: String? = null,
    @field:Size(max = 500, message = "메모는 500자 이하만 가능합니다.")
    val memo: String? = null,
)
