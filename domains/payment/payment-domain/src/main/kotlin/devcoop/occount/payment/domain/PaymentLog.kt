package devcoop.occount.payment.domain

import devcoop.occount.payment.domain.type.EventType
import devcoop.occount.payment.domain.type.PaymentType
import devcoop.occount.payment.domain.vo.CardInfo
import devcoop.occount.payment.domain.vo.PointTransaction
import devcoop.occount.payment.domain.vo.TransactionInfo
import java.time.LocalDateTime

class PaymentLog(
    private var paymentId: Long = 0L,
    private var userId: Long,
    private var paymentDate: LocalDateTime = LocalDateTime.now(),
    private var paymentType: PaymentType,
    private var totalAmount: Int,
    private var pointTransaction: PointTransaction? = null,
    private var cardInfo: CardInfo? = null,
    private var transactionInfo: TransactionInfo? = null,
    private var managedEmail: String? = null,
    private var eventType: EventType? = EventType.NONE,
) {
    fun getPaymentId(): Long = paymentId
    fun getUserId(): Long = userId
    fun getPaymentDate(): LocalDateTime = paymentDate
    fun getPaymentType(): PaymentType = paymentType
    fun getTotalAmount(): Int = totalAmount
    fun getPointTransaction(): PointTransaction? = pointTransaction
    fun getCardInfo(): CardInfo? = cardInfo
    fun getTransactionInfo(): TransactionInfo? = transactionInfo
    fun getManagedEmail(): String? = managedEmail
    fun getEventType(): EventType? = eventType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaymentLog) return false
        if (paymentId != other.paymentId) return false
        return true
    }

    override fun hashCode(): Int = paymentId.hashCode()
}
