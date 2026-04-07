package devcoop.occount.order.domain.order

import java.time.Instant

data class OrderAggregate(
    val orderId: String,
    val userId: Long,
    val lines: List<OrderLine>,
    val payment: OrderPayment,
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentStatus: OrderStepStatus = OrderStepStatus.PENDING,
    val stockStatus: OrderStepStatus = OrderStepStatus.PENDING,
    val cancelRequested: Boolean = false,
    val failureReason: String? = null,
    val expiresAt: Instant,
    val paymentLogId: Long? = null,
    val pointsUsed: Int = 0,
    val cardAmount: Int = 0,
    val transactionId: String? = null,
    val approvalNumber: String? = null,
    val paymentCompensationRequested: Boolean = false,
    val stockCompensationRequested: Boolean = false,
    val version: Long = 0L,
)
