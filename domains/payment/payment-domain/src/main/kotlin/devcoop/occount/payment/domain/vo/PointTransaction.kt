package devcoop.occount.payment.domain.vo

class PointTransaction(
    private var beforePoint: Int,
    private var transactionPoint: Int,
    private var afterPoint: Int,
) {
    fun beforePoint(): Int = beforePoint
    fun transactionPoint(): Int = transactionPoint
    fun afterPoint(): Int = afterPoint

    fun calculateAfterPoint(): Int {
        return beforePoint + transactionPoint
    }
}
