package devcoop.occount.investment.domain.investment

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvalidInvestmentAmountException : BusinessBaseException(ErrorMessage.INVALID_INVESTMENT_AMOUNT)
