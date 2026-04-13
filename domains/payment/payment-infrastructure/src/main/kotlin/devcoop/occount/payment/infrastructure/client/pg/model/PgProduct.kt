package devcoop.occount.payment.infrastructure.client.pg.model

data class PgProduct(
    val name: String,
    val quantity: Int,
    val total: Int,
)
