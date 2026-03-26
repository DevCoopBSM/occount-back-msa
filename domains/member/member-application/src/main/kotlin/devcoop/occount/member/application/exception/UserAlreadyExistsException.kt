package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class UserAlreadyExistsException : BusinessBaseException(ErrorMessage.USER_ALREADY_EXISTS)
