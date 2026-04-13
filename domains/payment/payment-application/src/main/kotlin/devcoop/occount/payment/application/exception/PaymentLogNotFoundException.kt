package devcoop.occount.payment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PaymentLogNotFoundException : BusinessBaseException(ErrorMessage.PAYMENT_LOG_NOT_FOUND)
