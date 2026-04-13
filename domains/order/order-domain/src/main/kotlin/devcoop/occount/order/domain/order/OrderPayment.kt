package devcoop.occount.order.domain.order

import devcoop.occount.core.common.event.OrderPaymentType

data class OrderPayment(
    val type: OrderPaymentType,
    val totalAmount: Int,
)
