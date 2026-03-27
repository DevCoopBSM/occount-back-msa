package devcoop.occount.payment.infrastructure.persistence

import devcoop.occount.payment.domain.type.RefundState
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
    private var paymentId: String? = null,
    @field:CreationTimestamp
    @field:Column(name = "charge_date", nullable = false, updatable = false)
    private var chargeDate: LocalDateTime = LocalDateTime.now(),
    @field:Column(name = "charge_amount", nullable = false)
    private var chargeAmount: Int = 0,
    @Embedded
    private var pointTransaction: PointTransactionJpaEmbeddable = PointTransactionJpaEmbeddable(),
    @Embedded
    private var cardInfo: CardInfoJpaEmbeddable? = null,
    @Embedded
    private var transactionInfo: TransactionInfoJpaEmbeddable? = null,
    @field:Column(name = "managed_email")
    private var managedEmail: String? = null,
    @field:Column(name = "reason")
    private var reason: String? = null,
    @Enumerated(EnumType.STRING)
    @field:Column(name = "refund_state", nullable = false)
    private var refundState: RefundState = RefundState.NONE,
    @field:Column(name = "refund_date")
    private var refundDate: LocalDateTime? = null,
    @field:Column(name = "refund_requester_id")
    private var refundRequesterId: String? = null,
) {
    fun getChargeId() = chargeId
    fun getUserId() = userId
    fun getPaymentId() = paymentId
    fun getChargeDate() = chargeDate
    fun getChargeAmount() = chargeAmount
    fun getPointTransaction() = pointTransaction
    fun getCardInfo() = cardInfo
    fun getTransactionInfo() = transactionInfo
    fun getManagedEmail() = managedEmail
    fun getReason() = reason
    fun getRefundState() = refundState
    fun getRefundDate() = refundDate
    fun getRefundRequesterId() = refundRequesterId
}
