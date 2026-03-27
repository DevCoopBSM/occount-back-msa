package devcoop.occount.db.payment

import devcoop.occount.payment.domain.ChargeLogRepository
import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.type.RefundState
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ChargeLogRepositoryImpl(
    private val persistenceRepository: ChargeLogPersistenceRepository
) : ChargeLogRepository {

    override fun findByUserId(userId: Long): List<ChargeLog> {
        return persistenceRepository.findByUserId(userId)
            .map(PaymentPersistenceMapper::toDomain)
    }

    override fun findByPaymentId(paymentId: String): ChargeLog? {
        return persistenceRepository.findByPaymentId(paymentId)
            ?.let(PaymentPersistenceMapper::toDomain)
    }

    override fun findByRefundState(refundState: RefundState): List<ChargeLog> {
        return persistenceRepository.findByRefundState(refundState)
            .map(PaymentPersistenceMapper::toDomain)
    }

    override fun findByUserIdAndChargeDateBetween(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<ChargeLog> {
        return persistenceRepository.findByUserIdAndChargeDateBetween(userId, startDate, endDate)
            .map(PaymentPersistenceMapper::toDomain)
    }

    override fun save(chargeLog: ChargeLog): ChargeLog {
        return persistenceRepository.save(PaymentPersistenceMapper.toEntity(chargeLog))
            .let(PaymentPersistenceMapper::toDomain)
    }

    override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> {
        return persistenceRepository.saveAll(chargeLogs.map(PaymentPersistenceMapper::toEntity))
            .map(PaymentPersistenceMapper::toDomain)
    }
}
