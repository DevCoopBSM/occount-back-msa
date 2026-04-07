package devcoop.occount.order.application.order

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class OrderCannotCancelException : BusinessBaseException(ErrorMessage.ORDER_CANNOT_CANCEL)
