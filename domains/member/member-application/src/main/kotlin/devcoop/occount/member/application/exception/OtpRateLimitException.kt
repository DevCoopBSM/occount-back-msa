package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class OtpRateLimitException : BusinessBaseException(ErrorMessage.OTP_RATE_LIMITED)
