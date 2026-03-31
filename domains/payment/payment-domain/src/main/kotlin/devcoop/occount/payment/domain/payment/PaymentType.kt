package devcoop.occount.payment.domain.payment

enum class PaymentType(
    private val description: String
) {
    POINT("포인트"),
    CARD("카드"),
    MIXED("포인트+카드");

    fun getDescription(): String {
        return description
    }
}
