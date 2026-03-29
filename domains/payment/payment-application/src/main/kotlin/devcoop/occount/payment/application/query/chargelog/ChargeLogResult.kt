package devcoop.occount.payment.application.query.chargelog

import devcoop.occount.payment.domain.ChargeLog
import java.time.LocalDateTime

data class ChargeLogResult(
    val chargeId: Long,
    val userId: Long,
    val chargeDate: LocalDateTime,
    val chargeAmount: Int,
    val paymentId: Long?,
    val beforePoint: Int,
    val afterPoint: Int,
    val reason: String?,
) {
    companion object {
        fun from(chargeLog: ChargeLog): ChargeLogResult {
            val pt = chargeLog.getPointTransaction()
            return ChargeLogResult(
                chargeId = chargeLog.getChargeId(),
                userId = chargeLog.getUserId(),
                chargeDate = chargeLog.getChargeDate(),
                chargeAmount = chargeLog.getChargeAmount(),
                paymentId = chargeLog.getPaymentId(),
                beforePoint = pt.beforePoint(),
                afterPoint = pt.afterPoint(),
                reason = chargeLog.getReason(),
            )
        }
    }
}
