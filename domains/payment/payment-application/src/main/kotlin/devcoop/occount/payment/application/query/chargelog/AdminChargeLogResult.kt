package devcoop.occount.payment.application.query.chargelog

import devcoop.occount.payment.application.shared.PointTransactionResult
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.ChargeReason
import java.time.LocalDateTime

data class AdminChargeLogResult(
    val chargeId: Long,
    val userId: Long,
    val chargeDate: LocalDateTime,
    val chargeReason: ChargeReason,
    val detailReason: String?,
    val chargedBy: Long?,
    val paymentId: Long?,
    val beforePoint: Int,
    val changeAmount: Int,
    val afterPoint: Int,
) {
    companion object {
        fun from(chargeLog: ChargeLog): AdminChargeLogResult {
            val transaction = PointTransactionResult.from(chargeLog.pointTransaction)
            return AdminChargeLogResult(
                chargeId = chargeLog.chargeId,
                userId = chargeLog.userId,
                chargeDate = chargeLog.chargeDate,
                chargeReason = chargeLog.chargeReason,
                detailReason = chargeLog.detailReason,
                chargedBy = chargeLog.chargedBy,
                paymentId = chargeLog.paymentId,
                beforePoint = transaction.beforePoint,
                changeAmount = transaction.changeAmount,
                afterPoint = transaction.afterPoint,
            )
        }
    }
}
