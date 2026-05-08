package devcoop.occount.payment.infrastructure.persistence.execution

import devcoop.occount.payment.application.output.OrderPaymentExecutionState
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface OrderPaymentExecutionPersistenceRepository : JpaRepository<OrderPaymentExecutionJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OrderPaymentExecutionJpaEntity e where e.orderId = :orderId")
    fun findByOrderIdForUpdate(@Param("orderId") orderId: Long): OrderPaymentExecutionJpaEntity?

    @Query("""
        select e.orderId from OrderPaymentExecutionJpaEntity e
        where e.state = :state and e.updatedAt < :updatedBefore
        order by e.updatedAt asc
    """)
    fun findStuckOrderIds(
        @Param("state") state: OrderPaymentExecutionState,
        @Param("updatedBefore") updatedBefore: LocalDateTime,
        pageable: Pageable,
    ): List<Long>
}
