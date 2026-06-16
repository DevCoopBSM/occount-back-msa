package devcoop.occount.investment.application.output

/**
 * PortOne 결제 단건 조회 포트. 웹훅으로 받은 paymentId 의 권위 있는 결제 상태/금액을 조회한다.
 */
interface PortOnePaymentPort {
    fun fetchPayment(paymentId: String): PortOnePayment
}

/**
 * PortOne 결제 조회 결과(필요한 필드만).
 * @property status 결제 상태(예: "PAID", "READY", "FAILED").
 * @property amount 실제 결제 금액(원).
 */
data class PortOnePayment(
    val status: String,
    val amount: Int,
) {
    fun isPaid(): Boolean = status == "PAID"
}
