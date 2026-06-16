package devcoop.occount.investment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvestmentAmountMismatchException : BusinessBaseException(ErrorMessage.INVESTMENT_AMOUNT_MISMATCH)
