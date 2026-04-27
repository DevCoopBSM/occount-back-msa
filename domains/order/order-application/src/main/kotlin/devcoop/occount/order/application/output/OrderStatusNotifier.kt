package devcoop.occount.order.application.output

import devcoop.occount.order.application.shared.OrderStreamEvent

interface OrderStatusNotifier {
    fun notify(event: OrderStreamEvent)
}
