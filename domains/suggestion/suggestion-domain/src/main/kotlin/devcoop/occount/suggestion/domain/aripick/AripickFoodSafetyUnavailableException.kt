package devcoop.occount.suggestion.domain.aripick

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class AripickFoodSafetyUnavailableException : BusinessBaseException(ErrorMessage.ARIPICK_FOOD_SAFETY_UNAVAILABLE)
