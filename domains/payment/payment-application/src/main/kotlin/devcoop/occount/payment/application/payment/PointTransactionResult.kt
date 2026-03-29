package devcoop.occount.payment.application.payment

import devcoop.occount.payment.domain.vo.PointTransaction

data class PointTransactionResult(
    val beforePoint: Int,
    val transactionPoint: Int,
    val afterPoint: Int,
) {
    companion object {
        fun from(pointTransaction: PointTransaction): PointTransactionResult {
            return PointTransactionResult(
                beforePoint = pointTransaction.beforePoint(),
                transactionPoint = pointTransaction.transactionPoint(),
                afterPoint = pointTransaction.afterPoint(),
            )
        }
    }
}
