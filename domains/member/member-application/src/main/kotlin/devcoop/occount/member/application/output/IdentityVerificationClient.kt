package devcoop.occount.member.application.output

data class VerifiedIdentity(
    val ciNumber: String,
    val username: String,
    val phone: String,
)

interface IdentityVerificationClient {
    fun verify(identityVerificationId: String): VerifiedIdentity
}
