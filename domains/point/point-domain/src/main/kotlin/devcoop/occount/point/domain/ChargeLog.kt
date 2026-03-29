package devcoop.occount.point.domain

import devcoop.occount.point.domain.vo.PointTransaction
import java.time.LocalDateTime

class ChargeLog(
    private var chargeId: Long = 0L,
    private var userId: Long,
    private var chargeDate: LocalDateTime = LocalDateTime.now(),
    private var chargeAmount: Int,
    private var paymentId: Long? = null,
    private var pointTransaction: PointTransaction,
    private var reason: String? = null,
) {
    fun getChargeId(): Long = chargeId
    fun getUserId(): Long = userId
    fun getChargeDate(): LocalDateTime = chargeDate
    fun getChargeAmount(): Int = chargeAmount
    fun getPaymentId(): Long? = paymentId
    fun getPointTransaction(): PointTransaction = pointTransaction
    fun getReason(): String? = reason

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChargeLog) return false
        if (chargeId != other.chargeId) return false
        return true
    }

    override fun hashCode(): Int = chargeId.hashCode()
}
