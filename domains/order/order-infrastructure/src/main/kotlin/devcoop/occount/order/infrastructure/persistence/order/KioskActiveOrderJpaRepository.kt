package devcoop.occount.order.infrastructure.persistence.order

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface KioskActiveOrderJpaRepository : JpaRepository<KioskActiveOrderJpaEntity, String> {

    /**
     * 행이 없을 때만 삽입한다(이미 있으면 무시). `INSERT IGNORE` 라 중복키 예외로 트랜잭션이
     * rollback-only 로 오염되지 않아, 뒤이은 잠금 조회/교체를 같은 트랜잭션에서 안전하게 이어갈 수 있다.
     */
    @Modifying
    @Query(
        value = """
            INSERT IGNORE INTO kiosk_active_order (kiosk_id, order_id, updated_at)
            VALUES (:kioskId, :orderId, :updatedAt)
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("kioskId") kioskId: String,
        @Param("orderId") orderId: Long,
        @Param("updatedAt") updatedAt: Instant,
    )

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from KioskActiveOrderJpaEntity e where e.kioskId = :kioskId")
    fun findByKioskIdForUpdate(@Param("kioskId") kioskId: String): KioskActiveOrderJpaEntity?
}
