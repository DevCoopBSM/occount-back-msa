package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class ContributionWithdrawalRequestNotFoundException :
    BusinessBaseException(ErrorMessage.CONTRIBUTION_WITHDRAWAL_REQUEST_NOT_FOUND)
