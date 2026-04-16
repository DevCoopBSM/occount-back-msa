package devcoop.occount.payment.domain.payment

import devcoop.occount.payment.domain.wallet.PointTransaction
import java.time.LocalDateTime

class PaymentLog(
    private var paymentId: Long = 0L,
    private var userId: Long?,
    private var paymentDate: LocalDateTime = LocalDateTime.now(),
    private var paymentType: PaymentType,
    private var totalAmount: Int,
    private var pointTransaction: PointTransaction? = null,
    private var cardInfo: CardInfo? = null,
    private var transactionInfo: TransactionInfo? = null,
    private var eventType: EventType? = EventType.NONE,
    private var refundState: RefundState = RefundState.NONE,
    private var refundDate: LocalDateTime? = null,
    private var refundRequesterId: String? = null,
) {
    fun getPaymentId(): Long = paymentId
    fun getUserId(): Long? = userId
    fun getPaymentDate(): LocalDateTime = paymentDate
    fun getPaymentType(): PaymentType = paymentType
    fun getTotalAmount(): Int = totalAmount
    fun getPointTransaction(): PointTransaction? = pointTransaction
    fun getCardInfo(): CardInfo? = cardInfo
    fun getTransactionInfo(): TransactionInfo? = transactionInfo
    fun getEventType(): EventType? = eventType
    fun getRefundState(): RefundState = refundState
    fun getRefundDate(): LocalDateTime? = refundDate
    fun getRefundRequesterId(): String? = refundRequesterId

    fun requestRefund(requesterId: String) {
        this.refundState = RefundState.REQUESTED
        this.refundRequesterId = requesterId
    }

    fun completeRefund() {
        this.refundState = RefundState.COMPLETED
        this.refundDate = LocalDateTime.now()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaymentLog) return false
        if (paymentId != other.paymentId) return false
        return true
    }

    override fun hashCode(): Int = paymentId.hashCode()
}
