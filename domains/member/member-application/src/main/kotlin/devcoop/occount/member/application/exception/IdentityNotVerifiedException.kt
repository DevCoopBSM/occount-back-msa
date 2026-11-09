package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class IdentityNotVerifiedException : BusinessBaseException(ErrorMessage.IDENTITY_NOT_VERIFIED)
