package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompensationFailedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.payment.application.exception.PaymentAlreadyCompletedException
import org.springframework.stereotype.Service

@Service
class CompensateOrderPaymentUseCase(
    private val eventPublisher: EventPublisher,
) {
    fun compensate(event: OrderPaymentCompensationRequestedEvent) {
        val ex = PaymentAlreadyCompletedException()
        eventPublisher.publish(
            topic = DomainTopics.ORDER_PAYMENT_COMPENSATION_FAILED,
            key = event.orderId,
            eventType = DomainEventTypes.ORDER_PAYMENT_COMPENSATION_FAILED,
            payload = OrderPaymentCompensationFailedEvent(
                orderId = event.orderId,
                userId = event.userId,
                reason = ex.message ?: "Payment compensation failed",
            ),
        )
    }
}
