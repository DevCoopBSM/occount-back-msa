package devcoop.occount.payment.infrastructure.persistence.paymentlog

import devcoop.occount.payment.domain.payment.PaymentType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PaymentLogPersistenceRepository : JpaRepository<PaymentLogJpaEntity, Long> {
    fun findByUserId(userId: Long): MutableList<PaymentLogJpaEntity>
    fun findByUserId(userId: Long, pageable: Pageable): Page<PaymentLogJpaEntity>
    fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): MutableList<PaymentLogJpaEntity>
    fun findByUserIdAndPaymentDateBetween(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable,
    ): Page<PaymentLogJpaEntity>
    fun findByPaymentType(paymentType: PaymentType): MutableList<PaymentLogJpaEntity>
}
