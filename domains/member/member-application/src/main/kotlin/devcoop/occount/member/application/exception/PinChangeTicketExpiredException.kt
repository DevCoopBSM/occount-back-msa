package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PinChangeTicketExpiredException : BusinessBaseException(ErrorMessage.PIN_CHANGE_TICKET_EXPIRED)
