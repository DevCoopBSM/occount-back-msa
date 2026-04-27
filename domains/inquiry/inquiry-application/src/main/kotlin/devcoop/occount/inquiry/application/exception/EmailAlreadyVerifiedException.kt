package devcoop.occount.inquiry.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class EmailAlreadyVerifiedException : BusinessBaseException(ErrorMessage.EMAIL_VERIFICATION_ALREADY_VERIFIED)
