package devcoop.occount.order.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.PaymentCompensatedEvent
import devcoop.occount.core.common.event.PaymentCompensationFailedEvent
import devcoop.occount.core.common.event.PaymentCompletedEvent
import devcoop.occount.core.common.event.PaymentFailedEvent
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.db.outbox.IdempotencyTracker
import devcoop.occount.order.application.exception.DuplicateEventException
import devcoop.occount.order.application.usecase.order.event.HandleOrderPaymentEventUseCase
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class OrderPaymentEventListener(
    private val handleOrderPaymentEventUseCase: HandleOrderPaymentEventUseCase,
    consumedEventRepository: ConsumedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    private val idempotency = IdempotencyTracker(CONSUMER_NAME, consumedEventRepository)

    @KafkaListener(topics = [DomainTopics.PAYMENT_EVENTS], groupId = CONSUMER_NAME)
    fun handlePaymentEvent(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
        @Header(DomainEventHeaders.EVENT_TYPE) eventType: String,
    ) {
        when (eventType) {
            DomainEventTypes.PAYMENT_COMPLETED -> handleOrderPaymentEventUseCase.applyCompletedPayment(
                event = objectMapper.readValue(payload, PaymentCompletedEvent::class.java),
                recordConsumption = { recordConsumption(eventId) },
            )
            DomainEventTypes.PAYMENT_FAILED -> handleOrderPaymentEventUseCase.applyFailedPayment(
                event = objectMapper.readValue(payload, PaymentFailedEvent::class.java),
                recordConsumption = { recordConsumption(eventId) },
            )
            DomainEventTypes.PAYMENT_COMPENSATED -> handleOrderPaymentEventUseCase.applyCompensatedPayment(
                event = objectMapper.readValue(payload, PaymentCompensatedEvent::class.java),
                recordConsumption = { recordConsumption(eventId) },
            )
            DomainEventTypes.PAYMENT_COMPENSATION_FAILED -> handleOrderPaymentEventUseCase.applyPaymentCompensationFailure(
                event = objectMapper.readValue(payload, PaymentCompensationFailedEvent::class.java),
                recordConsumption = { recordConsumption(eventId) },
            )
            else -> log.warn("알 수 없는 결제 이벤트 eventType={} eventId={}", eventType, eventId)
        }
    }

    private fun recordConsumption(eventId: String) {
        idempotency.recordOrThrowOnDuplicate(eventId) { throw DuplicateEventException() }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderPaymentEventListener::class.java)
        private const val CONSUMER_NAME = "payment-event-v1"
    }
}
