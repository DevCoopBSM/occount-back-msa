package devcoop.occount.payment.application.dto.response

data class PgResponse(
    val success: Boolean,
    val message: String?,
    val errorCode: String?,
    val transaction: TransactionInfo?,
    val card: CardInfo?,
    val additional: AdditionalInfo?,
    val rawResponse: String?
)
