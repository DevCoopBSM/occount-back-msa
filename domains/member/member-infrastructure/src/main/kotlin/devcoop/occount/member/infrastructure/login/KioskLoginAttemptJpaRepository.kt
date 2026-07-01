package devcoop.occount.member.infrastructure.login

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

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
        @Param("failCount") failCount: Int,
        @Param("lockedUntil") lockedUntil: Instant?,
        @Param("updatedAt") updatedAt: Instant,
    )
}
