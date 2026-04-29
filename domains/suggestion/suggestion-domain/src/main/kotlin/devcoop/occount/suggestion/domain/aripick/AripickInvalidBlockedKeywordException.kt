package devcoop.occount.suggestion.domain.aripick

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class AripickInvalidBlockedKeywordException : BusinessBaseException(ErrorMessage.ARIPICK_INVALID_BLOCKED_KEYWORD)
