package devcoop.occount.order.application.port

import devcoop.occount.order.application.shared.OrderStreamEvent

interface OrderStatusNotifier {
    fun notify(event: OrderStreamEvent)
}
