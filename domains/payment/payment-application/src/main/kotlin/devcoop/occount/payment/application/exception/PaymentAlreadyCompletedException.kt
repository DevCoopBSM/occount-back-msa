package devcoop.occount.payment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PaymentAlreadyCompletedException : BusinessBaseException(ErrorMessage.PAYMENT_ALREADY_COMPLETED)
