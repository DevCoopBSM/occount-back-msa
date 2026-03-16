package devcoop.occount.core.common.exception

import devcoop.occount.core.common.error.ErrorMessage

abstract class BusinessBaseException(
    val errorMessage: ErrorMessage,
) : RuntimeException(errorMessage.message)
