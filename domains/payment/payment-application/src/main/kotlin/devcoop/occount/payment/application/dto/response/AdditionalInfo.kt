package devcoop.occount.payment.application.dto.response

data class AdditionalInfo(
    val approvalStatus: String?,
    val approvalCode: String?,
    val icCreditApproval: String?,
    val transactionUuid: String?,
    val vanMessage: String?,
    val processingTime: Float?,
    val requestAt: String?,
    val responseAt: String?
)
