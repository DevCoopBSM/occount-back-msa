package devcoop.occount.payment.domain.wallet

data class PointTransaction(
    val beforePoint: Int,
    val changeAmount: Int,
    val afterPoint: Int,
) {

}
