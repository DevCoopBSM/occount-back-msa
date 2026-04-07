package devcoop.occount.order.application.order

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class OrderNotFoundException : BusinessBaseException(ErrorMessage.ORDER_NOT_FOUND)
