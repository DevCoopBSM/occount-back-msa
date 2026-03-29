package devcoop.occount.point.infrastructure.persistence

import devcoop.occount.point.domain.ChargeLog
import devcoop.occount.point.domain.ChargeLogRepository
import org.springframework.stereotype.Repository

@Repository
class ChargeLogRepositoryImpl(
    private val persistenceRepository: ChargeLogPersistenceRepository,
) : ChargeLogRepository {
    override fun findByUserId(userId: Long): List<ChargeLog> {
        return persistenceRepository.findByUserId(userId)
            .map(ChargePersistenceMapper::toDomain)
    }

    override fun findByPaymentId(paymentId: Long): ChargeLog? {
        return persistenceRepository.findByPaymentId(paymentId)
            ?.let(ChargePersistenceMapper::toDomain)
    }

    override fun save(chargeLog: ChargeLog): ChargeLog {
        return persistenceRepository.save(ChargePersistenceMapper.toEntity(chargeLog))
            .let(ChargePersistenceMapper::toDomain)
    }

    override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> {
        return persistenceRepository.saveAll(chargeLogs.map(ChargePersistenceMapper::toEntity))
            .map(ChargePersistenceMapper::toDomain)
    }
}
