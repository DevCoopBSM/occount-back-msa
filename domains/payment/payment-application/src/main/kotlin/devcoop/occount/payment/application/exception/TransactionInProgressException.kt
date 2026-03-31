package devcoop.occount.payment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class TransactionInProgressException : BusinessBaseException(ErrorMessage.TRANSACTION_IN_PROGRESS)
