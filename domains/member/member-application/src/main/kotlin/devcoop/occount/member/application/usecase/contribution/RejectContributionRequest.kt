package devcoop.occount.member.application.usecase.contribution

import jakarta.validation.constraints.Size

data class RejectContributionRequest(
    @field:Size(max = 500, message = "반려 사유는 500자 이하만 가능합니다.")
    val reason: String? = null,
)
