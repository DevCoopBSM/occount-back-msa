package devcoop.occount.member.domain.contribution

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class ContributionWithdrawalRequestAlreadyProcessedException :
    BusinessBaseException(ErrorMessage.CONTRIBUTION_WITHDRAWAL_REQUEST_ALREADY_PROCESSED)
