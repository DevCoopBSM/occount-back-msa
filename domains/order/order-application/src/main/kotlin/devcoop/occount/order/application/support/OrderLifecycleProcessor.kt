package devcoop.occount.order.application.support

import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.isFinalForClient
import org.springframework.stereotype.Component

@Component
class OrderLifecycleProcessor(
    private val orderCompensationScheduler: OrderCompensationScheduler,
    private val orderPendingResultRegistry: OrderPendingResultRegistry,
    private val orderResponseMapper: OrderResponseMapper,
) {
    fun processAfterOrderStateChange(order: OrderAggregate) {
        if (order.requiresCompensation()) {
            orderCompensationScheduler.scheduleRequiredCompensations(order.orderId)
        }

        completePendingOrderIfFinal(order)
    }

    fun completePendingOrderIfFinal(order: OrderAggregate) {
        if (order.status.isFinalForClient()) {
            orderPendingResultRegistry.completePendingOrder(order.orderId, orderResponseMapper.toResponse(order))
        }
    }
}
