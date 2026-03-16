package devcoop.occount.payment.domain.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class CardPaymentFailedException : BusinessBaseException(ErrorMessage.CARD_PAYMENT_FAILED)
