package devcoop.occount.member.domain.contribution

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class ContributionDepositRequestAlreadyProcessedException :
    BusinessBaseException(ErrorMessage.CONTRIBUTION_DEPOSIT_REQUEST_ALREADY_PROCESSED)
