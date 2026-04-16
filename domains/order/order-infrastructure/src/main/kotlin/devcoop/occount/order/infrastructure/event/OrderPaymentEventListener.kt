package devcoop.occount.order.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.OrderPaymentCompensatedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationFailedEvent
import devcoop.occount.core.common.event.OrderPaymentCompletedEvent
import devcoop.occount.core.common.event.OrderPaymentFailedEvent
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.order.application.exception.DuplicateEventException
import devcoop.occount.order.application.usecase.order.event.HandleOrderPaymentEventUseCase
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class OrderPaymentEventListener(
    private val handleOrderPaymentEventUseCase: HandleOrderPaymentEventUseCase,
    private val consumedEventRepository: ConsumedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    @KafkaListener(topics = [DomainTopics.ORDER_PAYMENT_COMPLETED], groupId = "order-payment-completed")
    fun onPaymentCompleted(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderPaymentEventUseCase.applyCompletedPayment(
            event = objectMapper.readValue(payload, OrderPaymentCompletedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-payment-completed", eventId) },
        )
    }

    @KafkaListener(topics = [DomainTopics.ORDER_PAYMENT_FAILED], groupId = "order-payment-failed")
    fun onPaymentFailed(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderPaymentEventUseCase.applyFailedPayment(
            event = objectMapper.readValue(payload, OrderPaymentFailedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-payment-failed", eventId) },
        )
    }

    @KafkaListener(topics = [DomainTopics.ORDER_PAYMENT_COMPENSATED], groupId = "order-payment-compensated")
    fun onPaymentCompensated(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderPaymentEventUseCase.applyCompensatedPayment(
            event = objectMapper.readValue(payload, OrderPaymentCompensatedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-payment-compensated", eventId) },
        )
    }

    @KafkaListener(topics = [DomainTopics.ORDER_PAYMENT_COMPENSATION_FAILED], groupId = "order-payment-compensation-failed")
    fun onPaymentCompensationFailed(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderPaymentEventUseCase.applyPaymentCompensationFailure(
            event = objectMapper.readValue(payload, OrderPaymentCompensationFailedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-payment-compensation-failed", eventId) },
        )
    }

    private fun saveConsumedEvent(consumerName: String, eventId: String) {
        try {
            consumedEventRepository.save(
                ConsumedEventJpaEntity(
                    id = "$consumerName:$eventId",
                    consumerName = consumerName,
                    eventId = eventId,
                ),
            )
        } catch (_: DataIntegrityViolationException) {
            throw DuplicateEventException()
        }
    }
}
