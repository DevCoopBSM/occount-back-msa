package devcoop.occount.member.domain.contribution

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvalidContributionWithdrawalAmountException :
    BusinessBaseException(ErrorMessage.INVALID_CONTRIBUTION_WITHDRAWAL_AMOUNT)
