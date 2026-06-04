package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.application.output.SalesRankingItem
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

    @Query(
        """
        SELECT new devcoop.occount.order.application.output.SalesRankingItem(
            l.itemId,
            MAX(l.itemNameSnapshot),
            SUM(l.quantity)
        )
        FROM OrderJpaEntity o
        JOIN o.lines l
        WHERE o.status = devcoop.occount.order.domain.order.OrderStatus.COMPLETED
          AND o.expiresAt >= :startDateTime
          AND o.expiresAt < :endDateTime
        GROUP BY l.itemId
        ORDER BY SUM(l.quantity) DESC, l.itemId ASC
        """
    )
    fun findPopularSalesRanking(
        @Param("startDateTime") startDateTime: Instant,
        @Param("endDateTime") endDateTime: Instant,
        pageable: Pageable,
    ): List<SalesRankingItem>

    @Query(
        """
        SELECT new devcoop.occount.order.application.output.SalesRankingItem(
            l.itemId,
            MAX(l.itemNameSnapshot),
            SUM(l.quantity)
        )
        FROM OrderJpaEntity o
        JOIN o.lines l
        WHERE o.status = devcoop.occount.order.domain.order.OrderStatus.COMPLETED
          AND o.expiresAt >= :startDateTime
          AND o.expiresAt < :endDateTime
        GROUP BY l.itemId
        ORDER BY SUM(l.quantity) ASC, l.itemId ASC
        """
    )
    fun findUnpopularSalesRanking(
        @Param("startDateTime") startDateTime: Instant,
        @Param("endDateTime") endDateTime: Instant,
        pageable: Pageable,
    ): List<SalesRankingItem>
}
