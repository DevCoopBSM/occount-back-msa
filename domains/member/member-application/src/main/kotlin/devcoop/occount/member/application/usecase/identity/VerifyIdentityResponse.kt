package devcoop.occount.member.application.usecase.identity

import java.time.LocalDate

data class VerifyIdentityResponse(
    val userCiNumber: String,
    val username: String,
    val userPhone: String,
    val birthDate: LocalDate?,
)
