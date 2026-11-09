package devcoop.occount.member.application.output

import java.time.LocalDate

data class VerifiedIdentity(
    val ciNumber: String,
    val username: String,
    val phone: String,
    val birthDate: LocalDate?,
)

interface IdentityVerificationClient {
    fun verify(identityVerificationId: String): VerifiedIdentity
}
