package devcoop.occount.payment.infrastructure.client.pg.model

data class PgResponse(
    val success: Boolean,
    val message: String?,
    val errorCode: String?,
    val transaction: PgTransaction?,
    val card: PgCard?,
    val additional: PgAdditional?,
    val rawResponse: String?,
)
