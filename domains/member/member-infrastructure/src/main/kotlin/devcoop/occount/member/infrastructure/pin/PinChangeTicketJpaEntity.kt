package devcoop.occount.member.infrastructure.pin

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(
    name = "pin_change_ticket",
    indexes = [Index(name = "idx_pin_change_ticket_user_id", columnList = "userId")],
)
class PinChangeTicketJpaEntity(
    @Id
    @Column(nullable = false, unique = true)
    val token: String,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val expiresAt: Instant,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
) {
    protected constructor() : this(
        token = "",
        userId = 0L,
        expiresAt = Instant.EPOCH,
    )
}
