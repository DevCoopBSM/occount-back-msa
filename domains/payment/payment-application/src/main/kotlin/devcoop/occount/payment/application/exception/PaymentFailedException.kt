package devcoop.occount.payment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PaymentFailedException : BusinessBaseException(ErrorMessage.PAYMENT_FAILED)
