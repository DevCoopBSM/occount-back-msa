package devcoop.occount.member.application.usecase.identity

data class VerifyIdentityResponse(
    val userCiNumber: String,
    val username: String,
    val userPhone: String,
)
