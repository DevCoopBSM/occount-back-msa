package devcoop.occount.inquiry.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PasswordResetTokenExpiredException : BusinessBaseException(ErrorMessage.PASSWORD_RESET_TOKEN_EXPIRED)
