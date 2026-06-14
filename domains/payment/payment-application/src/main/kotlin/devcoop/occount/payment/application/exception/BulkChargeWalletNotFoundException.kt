package devcoop.occount.payment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class BulkChargeWalletNotFoundException : BusinessBaseException(ErrorMessage.BULK_CHARGE_WALLET_NOT_FOUND)
