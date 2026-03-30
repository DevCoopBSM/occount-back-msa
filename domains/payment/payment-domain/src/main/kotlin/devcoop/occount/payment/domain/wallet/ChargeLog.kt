package devcoop.occount.payment.domain.wallet

import java.time.LocalDateTime

data class ChargeLog(
    val chargeId: Long = 0L,
    val userId: Long,
    val chargeDate: LocalDateTime = LocalDateTime.now(),
    val paymentId: Long? = null,
    val pointTransaction: PointTransaction,
    val chargeReason: ChargeReason,
    val detailReason: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChargeLog) return false
        if (chargeId != other.chargeId) return false
        return true
    }

    override fun hashCode(): Int = chargeId.hashCode()
}
