package devcoop.occount.payment.domain

import devcoop.occount.payment.domain.type.RefundState
import devcoop.occount.payment.domain.vo.CardInfo
import devcoop.occount.payment.domain.vo.PointTransaction
import devcoop.occount.payment.domain.vo.TransactionInfo
import java.time.LocalDateTime

class ChargeLog(
    private var chargeId: Long = 0L,
    private var userId: Long,
    private var chargeDate: LocalDateTime = LocalDateTime.now(),
    private var chargeAmount: Int,
    private var pointTransaction: PointTransaction,
    private var cardInfo: CardInfo? = null,
    private var transactionInfo: TransactionInfo? = null,
    private var managedEmail: String? = null,
    private var reason: String? = null,
    private var refundState: RefundState = RefundState.NONE,
    private var refundDate: LocalDateTime? = null,
    private var refundRequesterId: String? = null,
) {
    fun getChargeId(): Long = chargeId
    fun getUserId(): Long = userId
    fun getChargeDate(): LocalDateTime = chargeDate
    fun getChargeAmount(): Int = chargeAmount
    fun getPointTransaction(): PointTransaction = pointTransaction
    fun getCardInfo(): CardInfo? = cardInfo
    fun getTransactionInfo(): TransactionInfo? = transactionInfo
    fun getManagedEmail(): String? = managedEmail
    fun getReason(): String? = reason
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
        if (other !is ChargeLog) return false
        if (chargeId != other.chargeId) return false
        return true
    }

    override fun hashCode(): Int = chargeId.hashCode()
}
