package devcoop.occount.investment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvestmentNotFoundException : BusinessBaseException(ErrorMessage.INVESTMENT_NOT_FOUND)
