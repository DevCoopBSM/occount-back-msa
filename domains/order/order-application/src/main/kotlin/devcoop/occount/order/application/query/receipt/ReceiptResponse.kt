package devcoop.occount.order.application.query.receipt

import devcoop.occount.order.domain.order.OrderStatus

data class ReceiptResponse(
    val orderId: Long,
    val orderStatus: OrderStatus,
    val items: List<ReceiptItemResponse>,
    val payment: ReceiptPaymentResponse,
)
