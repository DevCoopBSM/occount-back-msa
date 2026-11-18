package devcoop.occount.order.application.support

import devcoop.occount.order.application.query.receipt.ReceiptItemResponse
import devcoop.occount.order.application.query.receipt.ReceiptPaymentResponse
import devcoop.occount.order.application.query.receipt.ReceiptPaymentType
import devcoop.occount.order.application.query.receipt.ReceiptResponse
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderLine
import org.springframework.stereotype.Component

@Component
class ReceiptResponseMapper {
    fun toResponse(order: OrderAggregate, paymentType: ReceiptPaymentType): ReceiptResponse {
        return ReceiptResponse(
            orderId = order.orderId,
            orderStatus = order.status,
            items = order.lines.map(::toItemResponse),
            payment = ReceiptPaymentResponse(
                type = paymentType,
                totalAmount = order.payment.totalAmount,
                pointsUsed = order.paymentResult.pointsUsed,
                cardAmount = order.paymentResult.cardAmount,
                approvalNumber = order.paymentResult.approvalNumber,
                transactionId = order.paymentResult.transactionId,
            ),
        )
    }

    private fun toItemResponse(line: OrderLine): ReceiptItemResponse {
        return ReceiptItemResponse(
            itemId = line.itemId,
            itemName = line.itemNameSnapshot,
            unitPrice = line.unitPrice,
            quantity = line.quantity,
            totalPrice = line.totalPrice,
        )
    }
}
