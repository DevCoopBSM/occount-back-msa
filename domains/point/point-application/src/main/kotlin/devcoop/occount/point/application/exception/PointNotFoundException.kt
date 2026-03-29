package devcoop.occount.point.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PointNotFound: BusinessBaseException(ErrorMessage.POINT_NOT_FOUND)
