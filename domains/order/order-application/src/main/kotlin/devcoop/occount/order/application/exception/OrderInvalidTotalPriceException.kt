package devcoop.occount.order.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class OrderInvalidTotalPriceException : BusinessBaseException(ErrorMessage.ORDER_INVALID_TOTAL_PRICE)
