package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.application.output.KioskActiveOrderRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class KioskActiveOrderRepositoryAdapter(
    private val kioskActiveOrderJpaRepository: KioskActiveOrderJpaRepository,
) : KioskActiveOrderRepository {

    @Transactional
    override fun claimActiveOrder(kioskId: String, newOrderId: Long): Long? {
        val now = Instant.now()

        // 1) 행이 없으면 newOrderId 로 생성(있으면 무시) → 이후 반드시 행이 존재한다.
        kioskActiveOrderJpaRepository.insertIfAbsent(kioskId, newOrderId, now)

        // 2) 행을 잠그고 이전 활성 주문을 읽는다. 같은 키오스크 동시 claim 은 여기서 직렬화된다.
        val active = kioskActiveOrderJpaRepository.findByKioskIdForUpdate(kioskId)
            ?: return null

        val displaced = active.getOrderId()
        if (displaced == newOrderId) {
            // 방금 이 주문으로 새로 만든 첫 주문 → 밀어낼 이전 주문 없음.
            return null
        }

        active.replaceOrder(newOrderId, now)
        return displaced
    }
}
