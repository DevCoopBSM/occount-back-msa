package devcoop.occount.member.infrastructure.login

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import kotlin.time.Instant

interface KioskLoginAttemptJpaRepository : JpaRepository<KioskLoginAttemptJpaEntity, String> {
    fun findByUserBarcode(userBarcode: String): KioskLoginAttemptJpaEntity?

    @Modifying
    @Transactional
    @Query(
        value = """
            INSERT INTO kiosk_login_attempt
                (user_barcode, fail_count, locked_until, updated_at)
            VALUES
                (:userBarcode, :failCount, :lockedUntil, :updatedAt)
            ON DUPLICATE KEY UPDATE
                fail_count = VALUES(fail_count),
                locked_until = VALUES(locked_until),
                updated_at = VALUES(updated_at)
        """, nativeQuery = true,
    )
    fun upsert(
        @Param("userBarcode") userBarcode: String,
        @Param("fail_count") failCount: Int,
        @Param("locked_until") lockedUntil: Instant?,
        @Param("updated_at") updatedAt: Instant,
    )
}
