package devcoop.occount.payment.application.query.chargelog

import devcoop.occount.payment.application.shared.PointTransactionResult
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.ChargeReason
import java.time.LocalDateTime

/**
 * 사용자 본인 충전 내역 조회 결과.
 * 관리자 감사 필드(chargedBy)와 본인을 가리키는 userId 는 노출하지 않는다.
 */
data class ChargeLogResult(
    val chargeId: Long,
    val chargeDate: LocalDateTime,
    val chargeReason: ChargeReason,
    val detailReason: String?,
    val paymentId: Long?,
    val beforePoint: Int,
    val changeAmount: Int,
    val afterPoint: Int,
) {
    companion object {
        fun from(chargeLog: ChargeLog): ChargeLogResult {
            val transaction = PointTransactionResult.from(chargeLog.pointTransaction)
            return ChargeLogResult(
                chargeId = chargeLog.chargeId,
                chargeDate = chargeLog.chargeDate,
                chargeReason = chargeLog.chargeReason,
                detailReason = chargeLog.detailReason,
                paymentId = chargeLog.paymentId,
                beforePoint = transaction.beforePoint,
                changeAmount = transaction.changeAmount,
                afterPoint = transaction.afterPoint,
            )
        }
    }
}
