package devcoop.occount.member.application.usecase.pin

import jakarta.validation.constraints.NotBlank

data class VerifyPasswordForPinChangeRequest(
    @field:NotBlank(message = "비밀번호는 비어있을 수 없습니다.")
    val password: String,
)
