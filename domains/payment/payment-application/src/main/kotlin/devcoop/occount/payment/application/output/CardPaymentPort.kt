package devcoop.occount.payment.application.output

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.VanResult

interface CardPaymentPort {
    fun approve(amount: Int, items: List<ItemCommand>): VanResult
}
