package devcoop.occount.point.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PointAlreadyInitializedException: BusinessBaseException(ErrorMessage.POINT_ALREADY_INITIALIZED_EXCEPTION)
