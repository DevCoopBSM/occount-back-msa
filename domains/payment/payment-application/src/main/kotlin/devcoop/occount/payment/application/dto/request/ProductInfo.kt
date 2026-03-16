package devcoop.occount.payment.application.dto.request

data class ProductInfo(
    val name: String,
    val price: Int,
    val quantity: Int,
    val total: Int
)
