package devcoop.occount.member.application.auth

interface TokenGenerator {
    fun createAccessToken(userId: Long, role: String): String
    fun createKioskToken(userId: Long, role: String): String
}
