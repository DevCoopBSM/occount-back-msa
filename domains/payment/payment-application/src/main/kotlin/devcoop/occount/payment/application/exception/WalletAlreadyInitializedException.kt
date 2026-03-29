package devcoop.occount.payment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class WalletAlreadyInitializedException : BusinessBaseException(ErrorMessage.POINT_ALREADY_INITIALIZED_EXCEPTION)
