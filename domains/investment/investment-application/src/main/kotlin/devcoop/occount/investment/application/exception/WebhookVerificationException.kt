package devcoop.occount.investment.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class WebhookVerificationException : BusinessBaseException(ErrorMessage.INVESTMENT_WEBHOOK_VERIFICATION_FAILED)
