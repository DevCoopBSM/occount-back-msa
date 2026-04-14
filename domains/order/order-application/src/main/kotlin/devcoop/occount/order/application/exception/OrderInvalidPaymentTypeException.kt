package devcoop.occount.order.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class OrderInvalidPaymentTypeException : BusinessBaseException(ErrorMessage.ORDER_INVALID_PAYMENT_TYPE)
