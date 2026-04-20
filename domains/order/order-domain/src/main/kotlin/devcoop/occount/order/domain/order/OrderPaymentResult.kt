package devcoop.occount.order.domain.order

data class OrderPaymentResult(
    val paymentLogId: Long? = null,
    val pointsUsed: Int = 0,
    val cardAmount: Int = 0,
    val transactionId: String? = null,
    val approvalNumber: String? = null,
)
