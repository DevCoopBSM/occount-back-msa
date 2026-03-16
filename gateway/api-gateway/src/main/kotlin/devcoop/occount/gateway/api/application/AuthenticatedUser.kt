package devcoop.occount.gateway.api.application

data class AuthenticatedUser(
    val userId: Long,
    val role: String,
)
