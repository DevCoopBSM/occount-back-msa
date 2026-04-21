package devcoop.occount.item.domain.item

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class ItemAlreadyExistsException : BusinessBaseException(ErrorMessage.ITEM_ALREADY_EXISTS)
