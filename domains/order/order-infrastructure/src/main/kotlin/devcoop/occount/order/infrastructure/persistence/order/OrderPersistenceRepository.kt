package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.domain.order.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface OrderPersistenceRepository : JpaRepository<OrderJpaEntity, String> {
    @Query(
        """
        select o.orderId
        from OrderJpaEntity o
        where o.expiresAt <= :now
          and o.status not in :finalStatuses
        order by o.expiresAt asc
        """
    )
    fun findExpiredOrderIds(
        @Param("now") now: Instant,
        @Param("finalStatuses") finalStatuses: Collection<OrderStatus>,
    ): List<String>
}
