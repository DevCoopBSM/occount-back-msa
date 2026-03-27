package devcoop.occount.payment.infrastructure.persistence

import devcoop.occount.payment.domain.type.EventType
import devcoop.occount.payment.domain.type.PaymentType
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
@Table(name = "payment_log")
class PaymentLogJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:Column(name = "payment_id")
    private var paymentId: Long = 0L,
    @field:Column(name = "user_id", nullable = false)
    private var userId: Long = 0L,
    @field:CreationTimestamp
    @field:Column(name = "payment_date", nullable = false, updatable = false)
    private var paymentDate: LocalDateTime = LocalDateTime.now(),
    @Enumerated(EnumType.STRING)
    @field:Column(name = "payment_type", nullable = false)
    private var paymentType: PaymentType = PaymentType.POINT,
    @field:Column(name = "total_amount", nullable = false)
    private var totalAmount: Int = 0,
    @Embedded
    private var pointTransaction: PointTransactionJpaEmbeddable? = null,
    @Embedded
    private var cardInfo: CardInfoJpaEmbeddable? = null,
    @Embedded
    private var transactionInfo: TransactionInfoJpaEmbeddable? = null,
    @field:Column(name = "managed_email")
    private var managedEmail: String? = null,
    @Enumerated(EnumType.STRING)
    @field:Column(name = "event_type")
    private var eventType: EventType? = EventType.NONE,
) {
    fun getPaymentId() = paymentId
    fun getUserId() = userId
    fun getPaymentDate() = paymentDate
    fun getPaymentType() = paymentType
    fun getTotalAmount() = totalAmount
    fun getPointTransaction() = pointTransaction
    fun getCardInfo() = cardInfo
    fun getTransactionInfo() = transactionInfo
    fun getManagedEmail() = managedEmail
    fun getEventType() = eventType
}
