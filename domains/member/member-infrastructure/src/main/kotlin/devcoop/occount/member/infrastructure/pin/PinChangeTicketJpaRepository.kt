package devcoop.occount.member.infrastructure.pin

import org.springframework.data.jpa.repository.JpaRepository

interface PinChangeTicketJpaRepository : JpaRepository<PinChangeTicketJpaEntity, String> {
    fun findByToken(token: String): PinChangeTicketJpaEntity?
    fun deleteByToken(token: String)
    fun deleteByUserId(userId: Long)
}
