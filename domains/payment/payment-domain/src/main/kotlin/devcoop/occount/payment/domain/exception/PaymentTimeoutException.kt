package devcoop.occount.payment.domain.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PaymentTimeoutException : BusinessBaseException(ErrorMessage.PAYMENT_TIMEOUT)
