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
            // 결제 커맨드(payment.command.v1)와 분리된 전용 토픽으로 발행한다.
            // 같은 토픽이면 취소가 진행 중 결제(30초 블로킹) 뒤에 큐잉되어 단말 중단이 늦는다.
            // 별도 토픽·리스너 스레드에서 처리해야 결제 대기 중 즉시 단말을 중단시킬 수 있다.
            topic = DomainTopics.PAYMENT_CANCEL_COMMANDS,
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
