package devcoop.occount.payment.application.payment

import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.type.RefundState
import java.time.LocalDateTime

data class ChargeLogResult(
    val chargeId: Long,
    val userId: Long,
    val chargeDate: LocalDateTime,
    val chargeAmount: Int,
    val pointTransaction: PointTransactionResult,
    val cardInfo: CardResult?,
    val transactionInfo: TransactionResult?,
    val managedEmail: String?,
    val reason: String?,
    val refundState: RefundState,
    val refundDate: LocalDateTime?,
    val refundRequesterId: String?,
) {
    companion object {
        fun from(chargeLog: ChargeLog): ChargeLogResult {
            return ChargeLogResult(
                chargeId = chargeLog.getChargeId(),
                userId = chargeLog.getUserId(),
                chargeDate = chargeLog.getChargeDate(),
                chargeAmount = chargeLog.getChargeAmount(),
                pointTransaction = PointTransactionResult.from(chargeLog.getPointTransaction()),
                cardInfo = chargeLog.getCardInfo()?.let(CardResult::from),
                transactionInfo = chargeLog.getTransactionInfo()?.let(TransactionResult::from),
                managedEmail = chargeLog.getManagedEmail(),
                reason = chargeLog.getReason(),
                refundState = chargeLog.getRefundState(),
                refundDate = chargeLog.getRefundDate(),
                refundRequesterId = chargeLog.getRefundRequesterId(),
            )
        }
    }
}
