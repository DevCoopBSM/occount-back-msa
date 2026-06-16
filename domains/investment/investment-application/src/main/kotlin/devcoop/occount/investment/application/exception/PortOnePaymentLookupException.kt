package devcoop.occount.investment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PortOnePaymentLookupException : BusinessBaseException(ErrorMessage.INVESTMENT_PAYMENT_LOOKUP_FAILED)
