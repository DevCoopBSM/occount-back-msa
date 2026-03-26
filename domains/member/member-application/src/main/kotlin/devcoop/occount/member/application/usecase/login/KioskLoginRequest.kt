package devcoop.occount.member.application.usecase.login

import jakarta.validation.constraints.NotBlank

data class KioskLoginRequest(
    @field:NotBlank(message = "바코드는 비어있을 수 없습니다.")
    val userBarcode: String,

    @field:NotBlank(message = "핀번호는 비어있을 수 없습니다.")
    val userPin: String,
)
