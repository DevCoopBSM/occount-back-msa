package devcoop.occount.payment.application.shared

import devcoop.occount.payment.domain.payment.PaymentType

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
    val message: String?,
    val paymentLogId: Long? = null,
) {
    companion object {
        fun forCharge(paymentLogId: Long, chargedAmount: Int, approvalNumber: String?, transactionId: String?): PaymentResponse {
            return PaymentResponse(
                status = "success",
                type = PaymentType.CARD,
                paymentLogId = paymentLogId,
                chargedAmount = chargedAmount,
                totalAmount = chargedAmount,
                approvalNumber = approvalNumber,
                transactionId = transactionId,
                remainingPoints = null,
                paymentAmount = null,
                pointsUsed = null,
                cardAmount = null,
                message = null,
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
            transactionId: String?,
            paymentLogId: Long? = null,
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
                message = null,
                paymentLogId = paymentLogId,
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
                transactionId = null,
            )
        }
    }
}
