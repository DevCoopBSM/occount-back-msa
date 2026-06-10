package devcoop.occount.member.application.usecase.password

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ChangePasswordRequest(
    @field:NotBlank(message = "이메일은 비어있을 수 없습니다.")
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    val email: String,

    @field:NotBlank(message = "비밀번호는 비어있을 수 없습니다.")
    @field:Size(min = 8, max = 16, message = "비밀번호는 최소 8자 이상 16자 이하여야 합니다.")
    val newPassword: String,
)
