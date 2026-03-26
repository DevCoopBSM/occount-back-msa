package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class UserNotFoundException : BusinessBaseException(ErrorMessage.USER_NOT_FOUND)
