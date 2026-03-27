package devcoop.occount.payment.infrastructure.persistence

import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.PaymentLogRepository
import devcoop.occount.payment.domain.type.PaymentType
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class PaymentLogRepositoryImpl(
    private val persistenceRepository: PaymentLogPersistenceRepository
) : PaymentLogRepository {

    override fun findByUserId(userId: Long): List<PaymentLog> {
        return persistenceRepository.findByUserId(userId)
            .map(PaymentPersistenceMapper::toDomain)
    }

    override fun findByUserIdAndPaymentDateBetween(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): List<PaymentLog> {
        return persistenceRepository.findByUserIdAndPaymentDateBetween(userId, startDate, endDate)
            .map(PaymentPersistenceMapper::toDomain)
    }

    override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> {
        return persistenceRepository.findByPaymentType(paymentType)
            .map(PaymentPersistenceMapper::toDomain)
    }

    override fun save(paymentLog: PaymentLog): PaymentLog {
        return persistenceRepository.save(PaymentPersistenceMapper.toEntity(paymentLog))
            .let(PaymentPersistenceMapper::toDomain)
    }

    override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> {
        return persistenceRepository.saveAll(paymentLogs.map(PaymentPersistenceMapper::toEntity))
            .map(PaymentPersistenceMapper::toDomain)
    }
}
