package devcoop.occount.inquiry.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InquiryAccessDeniedException : BusinessBaseException(ErrorMessage.INQUIRY_ACCESS_DENIED)
