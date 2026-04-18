package devcoop.occount.order.domain.order

data class RequestedOrderLine(
    val itemId: Long,
    val quantity: Int,
)
