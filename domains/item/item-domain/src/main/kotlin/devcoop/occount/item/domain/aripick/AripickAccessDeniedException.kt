package devcoop.occount.item.domain.aripick

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class AripickAccessDeniedException : BusinessBaseException(ErrorMessage.ARIPICK_ACCESS_DENIED)
