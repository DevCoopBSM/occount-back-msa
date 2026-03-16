package devcoop.occount.order.infrastructure.event

data class OrderRequestedEvent(
    val orderInfos: List<OrderItemPayload>,
)

data class OrderItemPayload(
    val itemId: Long,
    val quantity: Int,
)
