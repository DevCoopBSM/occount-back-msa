package devcoop.occount.member.api.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SendEmailOtpRequest(
    @field:NotBlank(message = "이메일은 비어있을 수 없습니다.")
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    val email: String,
)
