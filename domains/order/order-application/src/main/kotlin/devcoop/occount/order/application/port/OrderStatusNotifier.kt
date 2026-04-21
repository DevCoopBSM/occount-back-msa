package devcoop.occount.order.application.port

import devcoop.occount.order.domain.order.OrderStatus

interface OrderStatusNotifier {
    fun notify(orderId: String, status: OrderStatus, failureReason: String?)
}
