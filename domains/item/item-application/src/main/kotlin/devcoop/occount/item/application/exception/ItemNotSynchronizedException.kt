package devcoop.occount.item.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class ItemNotSynchronizedException : BusinessBaseException(ErrorMessage.ITEM_NOT_SYNCHRONIZED)
