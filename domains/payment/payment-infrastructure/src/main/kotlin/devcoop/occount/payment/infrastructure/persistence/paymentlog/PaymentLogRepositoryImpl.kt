package devcoop.occount.payment.infrastructure.persistence.paymentlog

import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.PaymentType
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PaymentLogRepositoryImpl(
    private val persistenceRepository: PaymentLogPersistenceRepository
) : PaymentLogRepository {
    override fun findById(paymentId: Long): PaymentLog? {
        return persistenceRepository.findById(paymentId)
            .map(PaymentLogPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun findByUserId(userId: Long): List<PaymentLog> {
        return persistenceRepository.findByUserId(userId)
            .map(PaymentLogPersistenceMapper::toDomain)
    }

    override fun findByUserIdAndPaymentDateBetween(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<PaymentLog> {
        return persistenceRepository.findByUserIdAndPaymentDateBetween(userId, startDate, endDate)
            .map(PaymentLogPersistenceMapper::toDomain)
    }

    override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> {
        return persistenceRepository.findByPaymentType(paymentType)
            .map(PaymentLogPersistenceMapper::toDomain)
    }

    override fun save(paymentLog: PaymentLog): PaymentLog {
        return persistenceRepository.save(PaymentLogPersistenceMapper.toEntity(paymentLog))
            .let(PaymentLogPersistenceMapper::toDomain)
    }

    override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> {
        return persistenceRepository.saveAll(paymentLogs.map(PaymentLogPersistenceMapper::toEntity))
            .map(PaymentLogPersistenceMapper::toDomain)
    }
}
