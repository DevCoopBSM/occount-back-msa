package devcoop.occount.payment.domain.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvalidPointAmountException : BusinessBaseException(ErrorMessage.INVALID_CHARGE_AMOUNT)
