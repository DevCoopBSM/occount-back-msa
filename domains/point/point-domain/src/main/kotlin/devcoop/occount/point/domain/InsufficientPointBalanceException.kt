package devcoop.occount.point.domain

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InsufficientPointBalanceException : BusinessBaseException(ErrorMessage.INSUFFICIENT_POINTS)
