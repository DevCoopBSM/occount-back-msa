package devcoop.occount.member.domain.user

data class AccountInfo(
    val email: String,
    val password: String,
    val role: Role,
    val pin: String,
) {
    fun matchesPassword(rawPassword: String, matches: (String, String) -> Boolean): Boolean {
        return matches(rawPassword, password)
    }

    fun matchesPin(rawPin: String, matches: (String, String) -> Boolean): Boolean {
        return matches(rawPin, pin)
    }
}
