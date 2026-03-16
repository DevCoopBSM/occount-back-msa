package devcoop.occount.payment.application.payment

import devcoop.occount.payment.domain.type.TransactionType

data class PaymentRequest(
    val type: TransactionType,
    val charge: ChargeRequest?,
    val payment: PaymentDetails?
)
