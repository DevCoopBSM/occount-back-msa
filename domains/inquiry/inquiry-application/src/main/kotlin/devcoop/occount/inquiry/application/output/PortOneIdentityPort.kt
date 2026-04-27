package devcoop.occount.inquiry.application.output

import devcoop.occount.inquiry.domain.portone.IdentityVerification

interface PortOneIdentityPort {
    fun fetchIdentityVerification(identityVerificationId: String): IdentityVerification
}
