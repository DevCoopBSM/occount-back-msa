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
    private var cardRefundState: RefundState = RefundState.NONE,
    private var pointRefundState: RefundState = RefundState.NONE,
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
    fun getCardRefundState(): RefundState = cardRefundState
    fun getPointRefundState(): RefundState = pointRefundState
    fun getRefundDate(): LocalDateTime? = refundDate
    fun getRefundRequesterId(): String? = refundRequesterId

    fun requestRefund(requesterId: String) {
        if (this.refundState == RefundState.NONE) {
            this.refundState = RefundState.REQUESTED
        }
        if (this.refundRequesterId == null) {
            this.refundRequesterId = requesterId
        }
    }

    fun requestCardRefund() {
        if (cardRefundState == RefundState.NONE) {
            cardRefundState = RefundState.REQUESTED
        }
    }

    fun completeCardRefund() {
        cardRefundState = RefundState.COMPLETED
    }

    fun requestPointRefund() {
        if (pointRefundState == RefundState.NONE) {
            pointRefundState = RefundState.REQUESTED
        }
    }

    fun completePointRefund() {
        pointRefundState = RefundState.COMPLETED
    }

    fun syncRefundState(
        requiresCardRefund: Boolean,
        requiresPointRefund: Boolean,
    ) {
        if (isRefundCompleted(requiresCardRefund, requiresPointRefund)) {
            refundState = RefundState.COMPLETED
            if (refundDate == null) {
                refundDate = LocalDateTime.now()
            }
        } else {
            refundState = RefundState.REQUESTED
        }
    }

    private fun isRefundCompleted(
        requiresCardRefund: Boolean,
        requiresPointRefund: Boolean,
    ): Boolean {
        val cardRefundCompleted = !requiresCardRefund || cardRefundState == RefundState.COMPLETED
        val pointRefundCompleted = !requiresPointRefund || pointRefundState == RefundState.COMPLETED
        return cardRefundCompleted && pointRefundCompleted
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaymentLog) return false
        if (paymentId != other.paymentId) return false
        return true
    }

    override fun hashCode(): Int = paymentId.hashCode()
}
