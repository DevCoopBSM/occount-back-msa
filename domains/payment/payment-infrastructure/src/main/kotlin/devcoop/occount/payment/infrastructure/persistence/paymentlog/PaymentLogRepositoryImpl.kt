package devcoop.occount.payment.infrastructure.persistence.paymentlog

import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.PaymentType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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

    override fun findByUserId(userId: Long, pageable: Pageable): Page<PaymentLog> {
        return persistenceRepository.findByUserId(userId, pageable)
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

    override fun findByUserIdAndPaymentDateBetween(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable,
    ): Page<PaymentLog> {
        return persistenceRepository.findByUserIdAndPaymentDateBetween(userId, startDate, endDate, pageable)
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
