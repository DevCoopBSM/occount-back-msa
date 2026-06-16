package devcoop.occount.investment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PendingInvestmentNotFoundException : BusinessBaseException(ErrorMessage.PENDING_INVESTMENT_NOT_FOUND)
