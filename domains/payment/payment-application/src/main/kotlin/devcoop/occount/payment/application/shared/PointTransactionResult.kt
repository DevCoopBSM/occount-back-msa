package devcoop.occount.payment.application.shared

import devcoop.occount.point.domain.vo.PointTransaction

data class PointTransactionResult(
    val beforePoint: Int,
    val afterPoint: Int,
) {
    companion object {
        fun from(pointTransaction: PointTransaction): PointTransactionResult {
            return PointTransactionResult(
                beforePoint = pointTransaction.beforePoint(),
                afterPoint = pointTransaction.afterPoint(),
            )
        }
    }
}
