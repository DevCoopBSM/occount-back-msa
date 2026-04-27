package devcoop.occount.inquiry.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PortOneApiException : BusinessBaseException(ErrorMessage.PORTONE_API_ERROR)
