package devcoop.occount.order.application.shared

import com.fasterxml.jackson.annotation.JsonInclude
import devcoop.occount.order.domain.order.OrderStatus

data class OrderResponse(
    val orderId: Long,
    val status: OrderStatus,
    val failureReason: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val guestOrderToken: String? = null,
)
