package devcoop.occount.order.application.order

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class OrderAccessDeniedException : BusinessBaseException(ErrorMessage.ORDER_ACCESS_DENIED)
