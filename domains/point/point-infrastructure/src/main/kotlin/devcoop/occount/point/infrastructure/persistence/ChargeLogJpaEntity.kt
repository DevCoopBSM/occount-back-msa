package devcoop.occount.point.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
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
    @field:Column(name = "charge_amount", nullable = false)
    private var chargeAmount: Int = 0,
    @Embedded
    private var pointTransaction: PointTransactionJpaEmbeddable = PointTransactionJpaEmbeddable(),
    @field:Column(name = "reason")
    private var reason: String? = null,
) {
    fun getChargeId() = chargeId
    fun getUserId() = userId
    fun getPaymentId() = paymentId
    fun getChargeDate() = chargeDate
    fun getChargeAmount() = chargeAmount
    fun getPointTransaction() = pointTransaction
    fun getReason() = reason
}
