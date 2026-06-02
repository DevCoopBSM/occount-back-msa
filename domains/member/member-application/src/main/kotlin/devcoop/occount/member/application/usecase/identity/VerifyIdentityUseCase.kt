package devcoop.occount.member.application.usecase.identity

import devcoop.occount.member.application.output.IdentityVerificationClient
import org.springframework.stereotype.Service

@Service
class VerifyIdentityUseCase(
    private val identityVerificationClient: IdentityVerificationClient,
) {
    fun verify(identityVerificationId: String): VerifyIdentityResponse {
        val identity = identityVerificationClient.verify(identityVerificationId)
        return VerifyIdentityResponse(
            userCiNumber = identity.ciNumber,
            username = identity.username,
            userPhone = identity.phone,
        )
    }
}
