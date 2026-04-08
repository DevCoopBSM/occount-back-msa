package devcoop.occount.order.application.order

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class OrderTransactionFailedException : BusinessBaseException(ErrorMessage.ORDER_TRANSACTION_FAILED)
