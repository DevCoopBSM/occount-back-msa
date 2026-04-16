package devcoop.occount.order.application.support

import devcoop.occount.order.domain.order.OrderAggregate
import org.springframework.stereotype.Component

@Component
class OrderLifecycleProcessor(
    private val orderCompensationScheduler: OrderCompensationScheduler,
) {
    fun processAfterOrderStateChange(order: OrderAggregate) {
        if (order.requiresCompensation()) {
            orderCompensationScheduler.scheduleRequiredCompensations(order.orderId)
        }
    }
}
