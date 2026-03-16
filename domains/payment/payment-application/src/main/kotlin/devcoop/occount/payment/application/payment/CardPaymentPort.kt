package devcoop.occount.payment.application.payment

import devcoop.occount.payment.application.dto.request.ProductInfo
import devcoop.occount.payment.application.dto.response.PgResponse

interface CardPaymentPort {
    fun approve(amount: Int, products: List<ProductInfo>): PgResponse
}
