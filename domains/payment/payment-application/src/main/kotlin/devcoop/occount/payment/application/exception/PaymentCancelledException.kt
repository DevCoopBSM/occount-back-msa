package devcoop.occount.payment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PaymentCancelledException : BusinessBaseException(ErrorMessage.PAYMENT_CANCELLED)
