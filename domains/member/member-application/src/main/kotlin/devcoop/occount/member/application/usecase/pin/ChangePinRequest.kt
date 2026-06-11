package devcoop.occount.member.application.usecase.pin

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class ChangePinRequest(
    @field:NotBlank(message = "인증 티켓은 비어있을 수 없습니다.")
    val ticket: String,

    @field:NotBlank(message = "PIN은 비어있을 수 없습니다.")
    @field:Pattern(regexp = "^\\d{4,6}$", message = "PIN은 4~6자리 숫자여야 합니다.")
    val newPin: String,
)
