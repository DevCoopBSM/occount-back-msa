package devcoop.occount.order.application.shared

import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.OrderStepStatus
import java.time.Instant

data class OrderHistoryItem(
    val orderId: Long,
    val status: OrderStatus,
    val orderDate: Instant?,
    val totalAmount: Int,
    val lines: List<OrderLineResult>,
    val payment: OrderPaymentInfo,
)

data class OrderLineResult(
    val itemId: Long,
    val itemNameSnapshot: String,
    val unitPrice: Int,
    val quantity: Int,
    val totalPrice: Int,
)

data class OrderPaymentInfo(
    val paymentLogId: Long?,
    val paymentStatus: OrderStepStatus,
    val pointsUsed: Int,
    val cardAmount: Int,
    val transactionId: String?,
    val approvalNumber: String?,
)
