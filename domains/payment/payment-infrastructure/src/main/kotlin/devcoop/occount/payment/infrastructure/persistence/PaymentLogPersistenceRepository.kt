package devcoop.occount.payment.infrastructure.persistence

import devcoop.occount.payment.domain.type.PaymentType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PaymentLogPersistenceRepository : JpaRepository<PaymentLogJpaEntity, Long> {
    fun findByUserId(userId: Long): MutableList<PaymentLogJpaEntity>
    fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): MutableList<PaymentLogJpaEntity>
    fun findByPaymentType(paymentType: PaymentType): MutableList<PaymentLogJpaEntity>
}
