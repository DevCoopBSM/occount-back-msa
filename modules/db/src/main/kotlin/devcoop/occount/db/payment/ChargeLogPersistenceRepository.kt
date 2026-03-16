package devcoop.occount.db.payment

import devcoop.occount.payment.domain.type.RefundState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ChargeLogPersistenceRepository : JpaRepository<ChargeLogJpaEntity, Long> {
    fun findByUserId(userId: Long): MutableList<ChargeLogJpaEntity>
    fun findByPaymentId(paymentId: String): ChargeLogJpaEntity?
    fun findByRefundState(refundState: RefundState): MutableList<ChargeLogJpaEntity>
    fun findByUserIdAndChargeDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): MutableList<ChargeLogJpaEntity>
}
