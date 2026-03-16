package devcoop.occount.payment.application.dto.request

data class PgRequest(
    val amount: Int,
    val products: List<ProductInfo>
)
