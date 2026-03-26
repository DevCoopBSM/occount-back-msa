package devcoop.occount.item.domain.item

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class ItemNotFoundException : BusinessBaseException(ErrorMessage.ITEM_NOT_FOUND)
