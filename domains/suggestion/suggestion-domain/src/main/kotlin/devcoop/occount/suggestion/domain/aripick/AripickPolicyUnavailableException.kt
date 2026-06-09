package devcoop.occount.suggestion.domain.aripick

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class AripickPolicyUnavailableException : BusinessBaseException(ErrorMessage.ARIPICK_POLICY_UNAVAILABLE)
