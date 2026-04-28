package devcoop.occount.payment.application.output

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.VanResult

interface CardPaymentPort {
    fun approve(amount: Int, items: List<ItemCommand>, kioskId: String, paymentKey: String? = null): VanResult

    fun refund(transactionId: String?, approvalNumber: String?, approvalDate: String, terminalId: String?, amount: Int, kioskId: String): VanResult

    fun requestPendingApprovalCancellation(paymentKey: String, kioskId: String)
}
