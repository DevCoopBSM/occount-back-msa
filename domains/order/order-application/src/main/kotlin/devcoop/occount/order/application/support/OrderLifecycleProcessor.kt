package devcoop.occount.order.application.support

import devcoop.occount.order.application.port.OrderStatusNotifier
import devcoop.occount.order.domain.order.OrderAggregate
import org.springframework.stereotype.Component

@Component
class OrderLifecycleProcessor(
    private val orderCompensationScheduler: OrderCompensationScheduler,
    private val orderStatusNotifier: OrderStatusNotifier,
    private val orderStreamEventMapper: OrderStreamEventMapper,
) {
    fun processAfterOrderStateChange(order: OrderAggregate) {
        if (order.requiresCompensation()) {
            orderCompensationScheduler.scheduleRequiredCompensations(order.orderId)
        }
        val reconciledOrder = order.reconcileStatus()
        try {
            orderStatusNotifier.notify(orderStreamEventMapper.toStreamEvent(reconciledOrder))
        } catch (_: Exception) {
            // SSE 알림 실패는 주문 처리에 영향을 주지 않음
        }
    }
}
