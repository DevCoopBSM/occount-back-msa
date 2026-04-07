package devcoop.occount.payment.application.dto.request

data class PgCancelCommand(
    val transactionId: String?,
    val approvalNumber: String?,
    val amount: Int,
)
