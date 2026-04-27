package devcoop.occount.inquiry.application.usecase.portone

import devcoop.occount.inquiry.application.output.PortOneIdentityPort
import devcoop.occount.inquiry.domain.portone.IdentityVerification
import org.springframework.stereotype.Service

@Service
class VerifyIdentityUseCase(
    private val portOneIdentityPort: PortOneIdentityPort,
) {
    fun verify(identityVerificationId: String): IdentityVerification {
        return portOneIdentityPort.fetchIdentityVerification(identityVerificationId)
    }
}
