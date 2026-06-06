package devcoop.occount.order.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class OrderInvalidSalesRankingPeriodException : BusinessBaseException(ErrorMessage.ORDER_INVALID_SALES_RANKING_PERIOD)
