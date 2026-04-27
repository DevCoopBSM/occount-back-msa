package devcoop.occount.inquiry.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PasswordResetTokenAlreadyUsedException : BusinessBaseException(ErrorMessage.PASSWORD_RESET_TOKEN_ALREADY_USED)
