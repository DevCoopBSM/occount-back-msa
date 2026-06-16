package devcoop.occount.investment.domain.investment

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class InvestmentAlreadyConfirmedException : BusinessBaseException(ErrorMessage.INVESTMENT_ALREADY_CONFIRMED)
