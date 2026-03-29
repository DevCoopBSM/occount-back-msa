package devcoop.occount.point.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChargeLogPersistenceRepository : JpaRepository<ChargeLogJpaEntity, Long> {
    fun findByUserId(userId: Long): MutableList<ChargeLogJpaEntity>
    fun findByPaymentId(paymentId: Long): ChargeLogJpaEntity?
}
