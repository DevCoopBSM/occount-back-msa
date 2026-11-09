package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class ContributionDepositRequestNotFoundException :
    BusinessBaseException(ErrorMessage.CONTRIBUTION_DEPOSIT_REQUEST_NOT_FOUND)
