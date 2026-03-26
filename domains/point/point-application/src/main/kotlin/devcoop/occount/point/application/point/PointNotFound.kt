package devcoop.occount.point.application.point

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PointNotFound: BusinessBaseException(ErrorMessage.POINT_NOT_FOUND)
