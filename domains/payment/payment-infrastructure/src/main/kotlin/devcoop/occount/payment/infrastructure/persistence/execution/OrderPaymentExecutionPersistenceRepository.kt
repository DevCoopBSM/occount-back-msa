package devcoop.occount.payment.infrastructure.persistence.execution

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface OrderPaymentExecutionPersistenceRepository : JpaRepository<OrderPaymentExecutionJpaEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OrderPaymentExecutionJpaEntity e where e.orderId = :orderId")
    fun findByOrderIdForUpdate(@Param("orderId") orderId: String): OrderPaymentExecutionJpaEntity?
}
