package devcoop.occount.investment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvestmentAccessDeniedException : BusinessBaseException(ErrorMessage.INVESTMENT_ACCESS_DENIED)
