package devcoop.occount.product.domain.item

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class ItemStockNegativeException : BusinessBaseException(ErrorMessage.ITEM_STOCK_NEGATIVE)
