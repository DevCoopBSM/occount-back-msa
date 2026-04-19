package devcoop.occount.item.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class DuplicateEventException : BusinessBaseException(ErrorMessage.DUPLICATE_EVENT)