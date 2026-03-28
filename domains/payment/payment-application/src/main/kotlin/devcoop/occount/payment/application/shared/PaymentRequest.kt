package devcoop.occount.payment.application.shared

import devcoop.occount.payment.domain.exception.InvalidPaymentRequestException
import devcoop.occount.payment.domain.type.TransactionType

data class PaymentRequest(
    val type: TransactionType,
    val charge: ChargeRequest?,
    val payment: PaymentDetails?
) {
    fun requireCharge(): ChargeRequest = charge ?: throw InvalidPaymentRequestException()
    fun requirePayment(): PaymentDetails = payment ?: throw InvalidPaymentRequestException()
}
