package devcoop.occount.order.application.shared

import com.fasterxml.jackson.annotation.JsonAlias
import devcoop.occount.core.common.event.OrderPaymentType

data class OrderRequest(
    @param:JsonAlias("orderInfos")
    val items: List<OrderItemRequest>,
    val paymentType: OrderPaymentType,
    val totalAmount: Int,
)
