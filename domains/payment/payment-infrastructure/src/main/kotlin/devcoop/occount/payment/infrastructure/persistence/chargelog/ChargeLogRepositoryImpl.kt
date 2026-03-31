package devcoop.occount.payment.infrastructure.persistence.chargelog

import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.domain.wallet.ChargeLog
import org.springframework.stereotype.Repository

@Repository
class ChargeLogRepositoryImpl(
    private val persistenceRepository: ChargeLogPersistenceRepository,
) : ChargeLogRepository {
    override fun findByUserId(userId: Long): List<ChargeLog> {
        return persistenceRepository.findByUserId(userId)
            .map(ChargeLogPersistenceMapper::toDomain)
    }

    override fun findByPaymentId(paymentId: Long): ChargeLog? {
        return persistenceRepository.findByPaymentId(paymentId)
            ?.let(ChargeLogPersistenceMapper::toDomain)
    }

    override fun save(chargeLog: ChargeLog): ChargeLog {
        return persistenceRepository.save(ChargeLogPersistenceMapper.toEntity(chargeLog))
            .let(ChargeLogPersistenceMapper::toDomain)
    }

    override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> {
        return persistenceRepository.saveAll(chargeLogs.map(ChargeLogPersistenceMapper::toEntity))
            .map(ChargeLogPersistenceMapper::toDomain)
    }
}
