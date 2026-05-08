package devcoop.occount.payment.infrastructure.persistence.chargelog

import org.springframework.data.jpa.repository.JpaRepository

interface ChargeLogPersistenceRepository : JpaRepository<ChargeLogJpaEntity, Long> {
    fun findByPaymentId(paymentId: Long): ChargeLogJpaEntity?
}
