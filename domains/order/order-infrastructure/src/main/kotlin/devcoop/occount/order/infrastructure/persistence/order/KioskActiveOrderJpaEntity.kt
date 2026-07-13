package devcoop.occount.order.infrastructure.persistence.order

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 키오스크당 "현재 진행 중인 주문 1건"을 추적하는 테이블.
 * kiosk_id 를 PK 로 두어 키오스크당 최대 1행만 존재하며, 새 주문이 들어오면 order_id 를 교체한다.
 */
@Entity
@Table(name = "kiosk_active_order")
class KioskActiveOrderJpaEntity(
    @Id
    @field:Column(name = "kiosk_id", nullable = false)
    private var kioskId: String = "",
    @field:Column(name = "order_id", nullable = false)
    private var orderId: Long = 0L,
    @field:Column(name = "updated_at", nullable = false)
    private var updatedAt: Instant = Instant.now(),
) {
    fun getOrderId(): Long = orderId

    fun replaceOrder(newOrderId: Long, now: Instant) {
        this.orderId = newOrderId
        this.updatedAt = now
    }
}
