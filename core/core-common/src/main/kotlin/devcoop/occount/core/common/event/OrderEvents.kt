package devcoop.occount.core.common.event

data class OrderRequestedEvent(
    val orderId: String,
    val userId: Long?,
    val kioskId: String,
    val items: List<OrderRequestedItemPayload>,
)

data class OrderRequestedItemPayload(
    val itemId: Long,
    val quantity: Int,
)

data class OrderPaymentRequestedEvent(
    val orderId: String,
    val kioskId: String,
    val userId: Long?,
    val payment: OrderPaymentPayload,
    val items: List<OrderItemPayload>,
)

data class OrderPaymentCancellationRequestedEvent(
    val orderId: String,
    val kioskId: String,
    val userId: Long?,
)

data class OrderPaymentPayload(
    val totalAmount: Int,
)

data class OrderItemPayload(
    val itemId: Long,
    val itemName: String,
    val itemPrice: Int,
    val quantity: Int,
    val totalPrice: Int,
)

data class OrderPaymentCompletedEvent(
    val orderId: String,
    val userId: Long?,
    val paymentLogId: Long,
    val pointsUsed: Int,
    val cardAmount: Int,
    val totalAmount: Int,
    val transactionId: String?,
    val approvalNumber: String?,
)

data class OrderPaymentFailedEvent(
    val orderId: String,
    val userId: Long?,
    val reason: String,
)

data class OrderStockCompletedEvent(
    val orderId: String,
    val items: List<OrderItemPayload>,
    val totalAmount: Int,
)

data class OrderStockFailedEvent(
    val orderId: String,
    val reason: String,
)

data class OrderPaymentCompensationRequestedEvent(
    val orderId: String,
    val userId: Long?,
    val paymentLogId: Long?,
    val pointsUsed: Int,
    val cardAmount: Int,
)

data class OrderPaymentCompensatedEvent(
    val orderId: String,
    val userId: Long?,
)

data class OrderPaymentCompensationFailedEvent(
    val orderId: String,
    val userId: Long?,
    val reason: String,
)

data class OrderStockCompensationRequestedEvent(
    val orderId: String,
    val items: List<OrderStockCompensationItemPayload>,
)

data class OrderStockCompensationItemPayload(
    val itemId: Long,
    val quantity: Int,
)

data class OrderStockCompensatedEvent(
    val orderId: String,
)

data class OrderStockCompensationFailedEvent(
    val orderId: String,
    val reason: String,
)
