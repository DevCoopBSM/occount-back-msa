package devcoop.occount.member.infrastructure.pin

import devcoop.occount.member.application.output.PinChangeTicketRepository
import devcoop.occount.member.application.pin.PinChangeTicket
import org.springframework.stereotype.Repository

@Repository
class PinChangeTicketRepositoryImpl(
    private val pinChangeTicketJpaRepository: PinChangeTicketJpaRepository,
) : PinChangeTicketRepository {

    override fun save(ticket: PinChangeTicket): PinChangeTicket {
        return pinChangeTicketJpaRepository.save(ticket.toEntity()).toDomain()
    }

    override fun findByToken(token: String): PinChangeTicket? {
        return pinChangeTicketJpaRepository.findByToken(token)?.toDomain()
    }

    override fun deleteByToken(token: String) {
        pinChangeTicketJpaRepository.deleteByToken(token)
    }

    override fun deleteByUserId(userId: Long) {
        pinChangeTicketJpaRepository.deleteByUserId(userId)
    }

    private fun PinChangeTicket.toEntity() = PinChangeTicketJpaEntity(
        token = token,
        userId = userId,
        expiresAt = expiresAt,
        createdAt = createdAt,
    )

    private fun PinChangeTicketJpaEntity.toDomain() = PinChangeTicket(
        token = token,
        userId = userId,
        expiresAt = expiresAt,
        createdAt = createdAt,
    )
}
