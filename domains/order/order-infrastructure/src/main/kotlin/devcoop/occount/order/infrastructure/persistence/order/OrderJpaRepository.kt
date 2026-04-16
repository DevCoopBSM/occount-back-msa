package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.domain.order.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface OrderJpaRepository : JpaRepository<OrderJpaEntity, String> {
    @Query("SELECT o.orderId FROM OrderJpaEntity o WHERE o.expiresAt <= :now AND o.status NOT IN :finalStatuses")
    fun findExpiredNonFinalOrderIds(
        @Param("now") now: Instant,
        @Param("finalStatuses") finalStatuses: List<OrderStatus>,
    ): List<String>
}
