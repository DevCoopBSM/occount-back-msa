package devcoop.occount.member.infrastructure.login

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "kiosk_login_attempt")
class KioskLoginAttemptJpaEntity(
    @Id
    @Column(nullable = false, unique = true)
    val userBarcode: String,

    @Column(nullable = false)
    val failCount: Int = 0,

    @Column(nullable = true)
    val lockedUntil: Instant? = null,

    @Column(nullable = false)
    val updatedAt: Instant = Instant.now(),
) {
    protected constructor() : this(userBarcode = "")
}
