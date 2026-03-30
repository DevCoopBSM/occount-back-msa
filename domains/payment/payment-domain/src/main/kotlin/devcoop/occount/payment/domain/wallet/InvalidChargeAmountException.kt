package devcoop.occount.payment.domain.wallet

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvalidChargeAmountException : BusinessBaseException(ErrorMessage.INVALID_CHARGE_AMOUNT)
