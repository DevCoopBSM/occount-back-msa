package devcoop.occount.payment.application.shared

import devcoop.occount.payment.application.exception.InvalidPaymentRequestException
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.domain.payment.TransactionType

data class PaymentRequest(
    val type: TransactionType,
    val payment: PaymentDetails?,
) {
    fun requirePayment(): PaymentDetails {
        return payment ?: throw InvalidPaymentRequestException()
    }
}
