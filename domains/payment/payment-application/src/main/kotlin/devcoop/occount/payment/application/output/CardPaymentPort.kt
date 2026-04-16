package devcoop.occount.payment.application.output

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.PgResult

interface CardPaymentPort {
    fun approve(amount: Int, items: List<ItemCommand>): PgResult

    fun cancel(transactionId: String?, approvalNumber: String?, approvalDate: String, amount: Int): PgResult
}
