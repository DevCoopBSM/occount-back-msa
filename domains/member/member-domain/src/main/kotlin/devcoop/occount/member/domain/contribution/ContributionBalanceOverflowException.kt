package devcoop.occount.member.domain.contribution

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class ContributionBalanceOverflowException : BusinessBaseException(ErrorMessage.CONTRIBUTION_BALANCE_OVERFLOW)
