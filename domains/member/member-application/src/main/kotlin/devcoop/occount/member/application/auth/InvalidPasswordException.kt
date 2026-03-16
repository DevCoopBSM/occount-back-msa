package devcoop.occount.member.application.auth

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvalidPasswordException : BusinessBaseException(ErrorMessage.INVALID_PASSWORD)
