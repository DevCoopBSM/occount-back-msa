package devcoop.occount.member.infrastructure.jwt

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvalidTokenException : BusinessBaseException(ErrorMessage.INVALID_TOKEN)
