package devcoop.occount.payment.application.shared

import devcoop.occount.payment.domain.wallet.PointTransaction

data class PointTransactionResult(
    val beforePoint: Int,
    val changeAmount: Int,
    val afterPoint: Int,
) {
    companion object {
        fun from(pointTransaction: PointTransaction): PointTransactionResult {
            return PointTransactionResult(
                beforePoint = pointTransaction.beforePoint,
                changeAmount = pointTransaction.changeAmount,
                afterPoint = pointTransaction.afterPoint,
            )
        }
    }
}
