package devcoop.occount.payment.infrastructure.client.pg.model

data class PgAdditional(
    val approvalStatus: String?,
    val approvalCode: String?,
    val icCreditApproval: String?,
    val transactionUuid: String?,
    val vanMessage: String?,
    val processingTime: Float?,
    val requestAt: String?,
    val responseAt: String?,
)
