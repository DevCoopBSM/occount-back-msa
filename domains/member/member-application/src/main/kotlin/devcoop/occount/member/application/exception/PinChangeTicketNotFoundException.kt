package devcoop.occount.member.application.exception

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException

class PinChangeTicketNotFoundException : BusinessBaseException(ErrorMessage.PIN_CHANGE_TICKET_NOT_FOUND)
