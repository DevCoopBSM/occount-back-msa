package devcoop.occount.order.application.query.receipt

data class ReceiptPaymentResponse(
    val type: ReceiptPaymentType,
    val totalAmount: Int,
    val pointsUsed: Int,
    val cardAmount: Int,
    val approvalNumber: String?,
    val transactionId: String?,
)
