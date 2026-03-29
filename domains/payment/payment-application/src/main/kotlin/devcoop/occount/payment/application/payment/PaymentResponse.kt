package devcoop.occount.payment.application.payment

import devcoop.occount.payment.domain.type.PaymentType

data class PaymentResponse(
    val status: String,
    val type: PaymentType?,
    val totalAmount: Int?,
    val chargedAmount: Int?,
    val paymentAmount: Int?,
    val pointsUsed: Int?,
    val cardAmount: Int?,
    val remainingPoints: Int?,
    val approvalNumber: String?,
    val transactionId: String?,
    val message: String?
) {
    companion object {
        fun forCharge(chargedAmount: Int, remainingPoints: Int, approvalNumber: String, transactionId: String): PaymentResponse {
            return PaymentResponse(
                status = "success",
                type = PaymentType.CARD,
                chargedAmount = chargedAmount,
                totalAmount = chargedAmount,
                remainingPoints = remainingPoints,
                approvalNumber = approvalNumber,
                transactionId = transactionId,
                paymentAmount = null,
                pointsUsed = null,
                cardAmount = null,
                message = null
            )
        }

        fun forPayment(
            type: PaymentType,
            totalAmount: Int,
            paymentAmount: Int,
            pointsUsed: Int,
            cardAmount: Int?,
            remainingPoints: Int,
            approvalNumber: String?,
            transactionId: String?
        ): PaymentResponse {
            return PaymentResponse(
                status = "success",
                type = type,
                totalAmount = totalAmount,
                paymentAmount = paymentAmount,
                pointsUsed = pointsUsed,
                cardAmount = cardAmount,
                remainingPoints = remainingPoints,
                approvalNumber = approvalNumber,
                transactionId = transactionId,
                chargedAmount = null,
                message = null
            )
        }

        fun error(message: String): PaymentResponse {
            return PaymentResponse(
                status = "error",
                message = message,
                type = null,
                totalAmount = null,
                chargedAmount = null,
                paymentAmount = null,
                pointsUsed = null,
                cardAmount = null,
                remainingPoints = null,
                approvalNumber = null,
                transactionId = null
            )
        }
    }
}
