package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PointNotFoundException : BusinessBaseException(ErrorMessage.POINT_NOT_FOUND)
