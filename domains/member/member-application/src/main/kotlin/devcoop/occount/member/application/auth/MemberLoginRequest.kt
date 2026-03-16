package devcoop.occount.member.application.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class MemberLoginRequest(
    @field:NotBlank(message = "이메일은 비어있을 수 없습니다.")
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    val userEmail: String,

    @field:NotBlank(message = "비밀번호는 비어있을 수 없습니다.")
    val password: String,
)
