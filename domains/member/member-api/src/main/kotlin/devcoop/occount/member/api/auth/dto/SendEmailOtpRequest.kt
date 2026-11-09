package devcoop.occount.member.api.auth.dto

import devcoop.occount.member.application.otp.OtpPurpose
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class SendEmailOtpRequest(
    @field:NotBlank(message = "이메일은 비어있을 수 없습니다.")
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    val email: String,

    // 미지정 시 회원가입용으로 간주(기존 클라이언트 호환). 비밀번호 변경은 PASSWORD_RESET로 발송해야 한다.
    val purpose: OtpPurpose = OtpPurpose.REGISTER,
)
