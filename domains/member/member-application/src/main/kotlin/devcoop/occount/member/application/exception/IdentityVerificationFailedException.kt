package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class IdentityVerificationFailedException : BusinessBaseException(ErrorMessage.IDENTITY_VERIFICATION_FAILED)
