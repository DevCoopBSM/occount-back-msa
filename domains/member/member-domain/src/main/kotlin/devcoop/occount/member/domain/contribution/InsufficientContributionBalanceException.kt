package devcoop.occount.member.domain.contribution

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InsufficientContributionBalanceException : BusinessBaseException(ErrorMessage.INSUFFICIENT_CONTRIBUTION_BALANCE)
