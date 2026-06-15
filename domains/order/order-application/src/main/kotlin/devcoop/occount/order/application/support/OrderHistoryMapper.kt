package devcoop.occount.order.application.support

import devcoop.occount.order.application.shared.OrderHistoryItem
import devcoop.occount.order.application.shared.OrderLineResult
import devcoop.occount.order.application.shared.OrderPaymentInfo
import devcoop.occount.order.domain.order.OrderAggregate
import org.springframework.stereotype.Component

@Component
class OrderHistoryMapper {
    fun toHistoryItem(order: OrderAggregate): OrderHistoryItem {
        return OrderHistoryItem(
            orderId = order.orderId,
            status = order.status,
            orderDate = order.createdAt,
            totalAmount = order.payment.totalAmount,
            lines = order.lines.map { line ->
                OrderLineResult(
                    itemId = line.itemId,
                    itemNameSnapshot = line.itemNameSnapshot,
                    unitPrice = line.unitPrice,
                    quantity = line.quantity,
                    totalPrice = line.totalPrice,
                )
            },
            payment = OrderPaymentInfo(
                paymentLogId = order.paymentResult.paymentLogId,
                paymentStatus = order.paymentStatus,
                pointsUsed = order.paymentResult.pointsUsed,
                cardAmount = order.paymentResult.cardAmount,
                transactionId = order.paymentResult.transactionId,
                approvalNumber = order.paymentResult.approvalNumber,
            ),
        )
    }
}
