package devcoop.occount.payment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class KioskTerminalNotFoundException : BusinessBaseException(ErrorMessage.KIOSK_TERMINAL_NOT_FOUND)