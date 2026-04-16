package devcoop.occount.payment.application.shared

import devcoop.occount.payment.application.exception.InvalidPaymentRequestException

data class PaymentRequest(
    val payment: PaymentDetails?,
) {
    fun requirePayment(): PaymentDetails = payment ?: throw InvalidPaymentRequestException()
}
