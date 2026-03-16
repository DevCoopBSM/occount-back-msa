package devcoop.occount.core.common.auth

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvalidAuthenticatedRequestException : BusinessBaseException(ErrorMessage.INVALID_TOKEN)
