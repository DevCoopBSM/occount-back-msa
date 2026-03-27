package devcoop.occount.payment.application.payment

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.PgResult

interface CardPaymentPort {
    fun approve(amount: Int, items: List<ItemCommand>): PgResult
}
