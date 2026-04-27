package devcoop.occount.inquiry.domain.portone

data class IdentityVerification(
    val identityVerificationId: String,
    val name: String,
    val birthDate: String,
    val phoneNumber: String,
    val gender: String?,
    val ci: String?,
)
