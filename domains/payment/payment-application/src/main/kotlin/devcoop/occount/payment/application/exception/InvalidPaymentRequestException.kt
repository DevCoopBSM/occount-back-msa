package devcoop.occount.payment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvalidPaymentRequestException : BusinessBaseException(ErrorMessage.INVALID_PAYMENT_REQUEST)
