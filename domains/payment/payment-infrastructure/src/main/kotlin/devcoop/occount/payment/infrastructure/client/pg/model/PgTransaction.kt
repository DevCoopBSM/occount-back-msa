package devcoop.occount.payment.infrastructure.client.pg.model

data class PgTransaction(
    val messageNumber: String?,
    val typeCode: String?,
    val cardNumber: String?,
    val amount: Int?,
    val installmentMonths: Int?,
    val cancelType: String?,
    val approvalNumber: String?,
    val approvalDate: String?,
    val approvalTime: String?,
    val transactionId: String?,
    val terminalId: String?,
    val merchantNumber: String?,
    val rejectCode: String?,
    val rejectMessage: String?,
)
