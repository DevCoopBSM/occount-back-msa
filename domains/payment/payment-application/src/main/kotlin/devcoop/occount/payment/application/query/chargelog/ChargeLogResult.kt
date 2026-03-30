package devcoop.occount.payment.application.query.chargelog

import devcoop.occount.payment.domain.wallet.ChargeLog
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
            return ChargeLogResult(
                chargeId = chargeLog.chargeId,
                userId = chargeLog.userId,
                chargeDate = chargeLog.chargeDate,
                paymentId = chargeLog.paymentId,
                beforePoint = chargeLog.pointTransaction.beforePoint,
                chargeAmount = chargeLog.pointTransaction.changeAmount,
                afterPoint = chargeLog.pointTransaction.afterPoint,
                reason = chargeLog.detailReason,
            )
        }
    }
}
