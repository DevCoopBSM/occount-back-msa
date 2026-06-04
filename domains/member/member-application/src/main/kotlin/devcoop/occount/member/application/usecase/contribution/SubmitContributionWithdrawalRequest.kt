package devcoop.occount.member.application.usecase.contribution

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class SubmitContributionWithdrawalRequest(
    @field:Positive(message = "출자금 반환 신청 금액은 0보다 커야 합니다.")
    val amount: Int,
    @field:Size(max = 100, message = "은행명은 100자 이하만 가능합니다.")
    val bankName: String? = null,
    @field:Size(max = 100, message = "계좌번호는 100자 이하만 가능합니다.")
    val accountNumber: String? = null,
    @field:Size(max = 100, message = "예금주는 100자 이하만 가능합니다.")
    val accountHolder: String? = null,
    @field:Size(max = 500, message = "메모는 500자 이하만 가능합니다.")
    val memo: String? = null,
)
