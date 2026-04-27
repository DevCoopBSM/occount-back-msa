package devcoop.occount.inquiry.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class EmailVerificationCodeInvalidException : BusinessBaseException(ErrorMessage.EMAIL_VERIFICATION_CODE_INVALID)
