package devcoop.occount.member.api.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class VerifyEmailOtpRequest(
    @field:NotBlank(message = "이메일은 비어있을 수 없습니다.")
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    val email: String,

    @field:NotBlank(message = "인증번호는 비어있을 수 없습니다.")
    @field:Size(min = 6, max = 6, message = "인증번호는 6자리여야 합니다.")
    val otpCode: String,
)
