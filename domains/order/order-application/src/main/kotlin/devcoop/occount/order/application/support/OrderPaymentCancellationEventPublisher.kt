package devcoop.occount.order.application.support

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCancellationRequestedEvent
import devcoop.occount.order.domain.order.OrderAggregate
import org.springframework.stereotype.Component

@Component
class OrderPaymentCancellationEventPublisher(
    private val eventPublisher: EventPublisher,
) {
    fun publish(order: OrderAggregate) {
        eventPublisher.publish(
            topic = DomainTopics.ORDER_PAYMENT_CANCELLATION_REQUESTED,
            key = order.orderId.toString(),
            eventType = DomainEventTypes.ORDER_PAYMENT_CANCELLATION_REQUESTED,
            payload = OrderPaymentCancellationRequestedEvent(
                orderId = order.orderId,
                kioskId = order.kioskId,
                userId = order.userId,
            ),
        )
    }
}
