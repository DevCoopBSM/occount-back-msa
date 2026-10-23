package devcoop.occount.order.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class OrderReceiptNotAvailableException : BusinessBaseException(ErrorMessage.ORDER_RECEIPT_NOT_AVAILABLE)
