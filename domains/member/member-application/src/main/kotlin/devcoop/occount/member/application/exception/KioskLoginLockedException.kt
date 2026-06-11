package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class KioskLoginLockedException : BusinessBaseException(ErrorMessage.KIOSK_LOGIN_LOCKED)
