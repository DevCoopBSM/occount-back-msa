package devcoop.occount.order.application.shared

import com.fasterxml.jackson.annotation.JsonAlias

data class OrderRequest(
    @param:JsonAlias("orderInfos")
    val items: List<OrderItemRequest>,
    val totalAmount: Int,
    val kioskId: String,
)
