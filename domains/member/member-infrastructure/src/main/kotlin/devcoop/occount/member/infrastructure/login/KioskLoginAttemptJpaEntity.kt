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
    @Column(name = "user_barcode", nullable = false, unique = true)
    val userBarcode: String,

    @Column(name = "fail_count", nullable = false)
    val failCount: Int = 0,

    @Column(name = "locked_until", nullable = true)
    val lockedUntil: Instant? = null,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now(),
) {
    protected constructor() : this(userBarcode = "")
}
