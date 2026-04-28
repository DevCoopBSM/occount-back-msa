package devcoop.occount.inquiry.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InquiryNotFoundException : BusinessBaseException(ErrorMessage.INQUIRY_NOT_FOUND)
