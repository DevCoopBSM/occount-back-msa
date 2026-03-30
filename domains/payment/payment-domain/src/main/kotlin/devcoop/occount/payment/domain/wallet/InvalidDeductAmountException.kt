package devcoop.occount.payment.domain.wallet

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvalidDeductAmountException: BusinessBaseException(ErrorMessage.INVALID_DEDUCT_AMOUNT)
