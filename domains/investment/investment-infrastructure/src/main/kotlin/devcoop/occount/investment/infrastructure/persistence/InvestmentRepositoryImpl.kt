package devcoop.occount.investment.infrastructure.persistence

import devcoop.occount.investment.application.output.InvestmentRepository
import devcoop.occount.investment.domain.investment.Investment
import devcoop.occount.investment.domain.investment.InvestmentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class InvestmentRepositoryImpl(
    private val persistenceRepository: InvestmentPersistenceRepository,
) : InvestmentRepository {
    override fun findByPaymentId(paymentId: String): Investment? {
        return persistenceRepository.findByPaymentId(paymentId)
            ?.let(InvestmentPersistenceMapper::toDomain)
    }

    override fun findById(investmentId: Long): Investment? {
        return persistenceRepository.findById(investmentId)
            .map(InvestmentPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun findByUserId(userId: Long, pageable: Pageable): Page<Investment> {
        return persistenceRepository.findByUserId(userId, pageable)
            .map(InvestmentPersistenceMapper::toDomain)
    }

    override fun findAll(pageable: Pageable): Page<Investment> {
        return persistenceRepository.findAll(pageable)
            .map(InvestmentPersistenceMapper::toDomain)
    }

    override fun save(investment: Investment): Investment {
        return persistenceRepository.save(InvestmentPersistenceMapper.toEntity(investment))
            .let(InvestmentPersistenceMapper::toDomain)
    }

    override fun confirmIfPending(paymentId: String, confirmedAt: LocalDateTime): Boolean {
        val updated = persistenceRepository.confirmIfPending(
            paymentId = paymentId,
            confirmedAt = confirmedAt,
            pending = InvestmentStatus.PENDING,
            confirmed = InvestmentStatus.CONFIRMED,
        )
        return updated == 1
    }
}
