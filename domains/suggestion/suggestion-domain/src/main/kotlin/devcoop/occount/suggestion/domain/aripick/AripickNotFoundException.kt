package devcoop.occount.suggestion.domain.aripick

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class AripickNotFoundException : BusinessBaseException(ErrorMessage.ARIPICK_NOT_FOUND)
