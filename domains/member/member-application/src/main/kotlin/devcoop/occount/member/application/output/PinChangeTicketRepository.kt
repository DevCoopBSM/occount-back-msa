package devcoop.occount.member.application.output

import devcoop.occount.member.application.pin.PinChangeTicket

interface PinChangeTicketRepository {
    fun save(ticket: PinChangeTicket): PinChangeTicket
    fun findByToken(token: String): PinChangeTicket?
    fun deleteByToken(token: String)
    fun deleteByUserId(userId: Long)
}
