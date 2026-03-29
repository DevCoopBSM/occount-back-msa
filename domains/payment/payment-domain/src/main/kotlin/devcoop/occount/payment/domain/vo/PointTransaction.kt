package devcoop.occount.payment.domain.vo

class PointTransaction(
    private var beforePoint: Int,
    private var afterPoint: Int,
) {
    fun beforePoint(): Int = beforePoint
    fun afterPoint(): Int = afterPoint
}
