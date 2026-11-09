package devcoop.occount.payment.infrastructure.persistence.chargelog

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ChargeLogPersistenceRepository : JpaRepository<ChargeLogJpaEntity, Long> {
    fun findByPaymentId(paymentId: Long): ChargeLogJpaEntity?
    fun findByUserId(userId: Long, pageable: Pageable): Page<ChargeLogJpaEntity>
}
