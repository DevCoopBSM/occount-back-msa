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
            topic = DomainTopics.PAYMENT_COMMANDS,
            // 같은 단말(kioskId)의 결제·취소·보상이 같은 파티션에서 순서대로 처리되도록 kioskId로 파티셔닝.
            key = order.kioskId,
            eventType = DomainEventTypes.ORDER_PAYMENT_CANCELLATION_REQUESTED,
            payload = OrderPaymentCancellationRequestedEvent(
                orderId = order.orderId,
                kioskId = order.kioskId,
                userId = order.userId,
            ),
        )
    }
}
