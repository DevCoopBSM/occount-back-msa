package devcoop.occount.member.domain.user

data class AccountInfo(
    val email: String,
    val password: String,
    val role: Role,
    val pin: String,
)
