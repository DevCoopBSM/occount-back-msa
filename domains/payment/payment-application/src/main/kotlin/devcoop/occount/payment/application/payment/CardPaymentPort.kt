package devcoop.occount.payment.application.payment

import devcoop.occount.payment.application.dto.request.ItemInfo
import devcoop.occount.payment.application.dto.response.PgResponse

interface CardPaymentPort {
    fun approve(amount: Int, items: List<ItemInfo>): PgResponse
}
