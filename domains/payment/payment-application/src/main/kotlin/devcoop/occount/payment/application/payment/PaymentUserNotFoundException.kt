package devcoop.occount.payment.application.payment

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PaymentUserNotFoundException : BusinessBaseException(ErrorMessage.USER_NOT_FOUND)
