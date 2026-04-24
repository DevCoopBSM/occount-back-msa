package devcoop.occount.core.common.event

data class OrderRequestedEvent(
    val orderId: Long,
    val userId: Long?,
    val kioskId: String,
    val items: List<OrderRequestedItemPayload>,
)

data class OrderRequestedItemPayload(
    val itemId: Long,
    val quantity: Int,
)

data class OrderPaymentRequestedEvent(
    val orderId: Long,
    val kioskId: String,
    val userId: Long?,
    val payment: OrderPaymentPayload,
    val items: List<ItemStockPayload>,
)

data class OrderPaymentCancellationRequestedEvent(
    val orderId: Long,
    val kioskId: String,
    val userId: Long?,
)

data class OrderPaymentPayload(
    val totalAmount: Int,
)

data class ItemStockPayload(
    val itemId: Long,
    val itemName: String,
    val itemPrice: Int,
    val quantity: Int,
    val totalPrice: Int,
)

data class PaymentCompletedEvent(
    val orderId: Long,
    val userId: Long?,
    val paymentLogId: Long,
    val pointsUsed: Int,
    val cardAmount: Int,
    val totalAmount: Int,
    val transactionId: String?,
    val approvalNumber: String?,
)

data class PaymentFailedEvent(
    val orderId: Long,
    val userId: Long?,
    val reason: String,
)

data class ItemStockDecreasedEvent(
    val orderId: Long,
    val items: List<ItemStockPayload>,
    val totalAmount: Int,
)

data class ItemStockDecreaseFailedEvent(
    val orderId: Long,
    val reason: String,
)

data class OrderPaymentCompensationRequestedEvent(
    val orderId: Long,
    val kioskId: String,
    val userId: Long?,
    val paymentLogId: Long?,
    val pointsUsed: Int,
    val cardAmount: Int,
)

data class PaymentCompensatedEvent(
    val orderId: Long,
    val userId: Long?,
)

data class PaymentCompensationFailedEvent(
    val orderId: Long,
    val userId: Long?,
    val reason: String,
)

data class OrderStockCompensationRequestedEvent(
    val orderId: Long,
    val items: List<ItemStockCompensationPayload>,
)

data class ItemStockCompensationPayload(
    val itemId: Long,
    val quantity: Int,
)

data class ItemStockCompensatedEvent(
    val orderId: Long,
)

data class ItemStockCompensationFailedEvent(
    val orderId: Long,
    val reason: String,
)
