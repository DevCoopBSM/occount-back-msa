package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.domain.order.OrderStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface OrderJpaRepository : JpaRepository<OrderJpaEntity, Long> {
    @Query("SELECT o.orderId FROM OrderJpaEntity o WHERE o.expiresAt <= :now AND o.status NOT IN :finalStatuses")
    fun findExpiredNonFinalOrderIds(
        @Param("now") now: Instant,
        @Param("finalStatuses") finalStatuses: List<OrderStatus>,
    ): List<Long>

    @Query(
        """
        SELECT o.orderId FROM OrderJpaEntity o
        WHERE (o.cancelRequested = true
               OR o.paymentStatus = devcoop.occount.order.domain.order.OrderStepStatus.FAILED
               OR o.stockStatus = devcoop.occount.order.domain.order.OrderStepStatus.FAILED
               OR o.status = devcoop.occount.order.domain.order.OrderStatus.TIMED_OUT)
          AND (
                (o.paymentStatus = devcoop.occount.order.domain.order.OrderStepStatus.SUCCEEDED
                 AND o.paymentCompensationRequested = false)
                OR
                (o.stockStatus = devcoop.occount.order.domain.order.OrderStepStatus.SUCCEEDED
                 AND o.stockCompensationRequested = false)
              )
        ORDER BY o.orderId ASC
        """
    )
    fun findOrderIdsRequiringCompensation(pageable: Pageable): List<Long>
}
