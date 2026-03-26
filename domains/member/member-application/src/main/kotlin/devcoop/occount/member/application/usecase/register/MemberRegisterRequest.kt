package devcoop.occount.member.application.usecase.register

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class MemberRegisterRequest(
    @field:NotBlank(message = "유저 Ci번호는 비어있을 수 없습니다.")
    val userCiNumber: String,

    @field:NotBlank(message = "유저 이름은 비어있을 수 없습니다.")
    @field:Size(max = 55, message = "유저 이름은 55자 이하만 가능합니다.")
    val userName: String,

    val userAddress: String?,

    val userPhone: String?,

    @field:NotBlank(message = "유저 이메일은 비어있을 수 없습니다.")
    @field:Email(message = "올바른 이메일 형식이어야 합니다.")
    val userEmail: String,

    @field:NotBlank(message = "비밀번호는 비어있을 수 없습니다.")
    @field:Size(min = 8, max = 16, message = "비밀번호는 최소 8자 이상 16자 이하여야 합니다.")
    val password: String,
)
