package devcoop.occount.order.application.order

import devcoop.occount.core.common.event.OrderPaymentType

class OrderRequest(
    val orderInfos: List<OrderInfo>,
    val paymentType: OrderPaymentType,
    val totalAmount: Int,
)
