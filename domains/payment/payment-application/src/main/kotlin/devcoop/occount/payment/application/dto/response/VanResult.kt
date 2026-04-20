package devcoop.occount.payment.application.dto.response

data class VanResult(
    val success: Boolean,
    val message: String?,
    val errorCode: String?,
    val transaction: TransactionResult?,
    val card: CardResult?,
    val additional: AdditionalResult?,
    val rawResponse: String?
)
