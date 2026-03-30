package devcoop.occount.payment.infrastructure.persistence.chargelog

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "charge_log")
class ChargeLogJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:Column(name = "charge_id")
    private var chargeId: Long = 0L,
    @field:Column(name = "user_id", nullable = false)
    private var userId: Long = 0L,
    @field:Column(name = "payment_id")
    private var paymentId: Long? = null,
    @field:CreationTimestamp
    @field:Column(name = "charge_date", nullable = false, updatable = false)
    private var chargeDate: LocalDateTime = LocalDateTime.now(),
    @Embedded
    private var pointTransaction: PointTransactionJpaEmbeddable = PointTransactionJpaEmbeddable(),
    @field:Column(name = "reason")
    private var reason: String? = null,
) {
    fun getChargeId() = chargeId
    fun getUserId() = userId
    fun getPaymentId() = paymentId
    fun getChargeDate() = chargeDate
    fun getPointTransaction() = pointTransaction
    fun getReason() = reason
}
