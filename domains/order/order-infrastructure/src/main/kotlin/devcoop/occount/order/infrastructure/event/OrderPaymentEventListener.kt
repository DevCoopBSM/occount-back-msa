package devcoop.occount.order.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.OrderPaymentCompensatedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationFailedEvent
import devcoop.occount.core.common.event.OrderPaymentCompletedEvent
import devcoop.occount.core.common.event.OrderPaymentFailedEvent
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.order.application.order.OrderService
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class OrderPaymentEventListener(
    private val orderService: OrderService,
    private val consumedEventRepository: ConsumedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_PAYMENT_COMPLETED],
        groupId = "order-payment-completed",
    )
    fun onPaymentCompleted(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("order-payment-completed", eventId) {
            orderService.handlePaymentCompleted(objectMapper.readValue(payload, OrderPaymentCompletedEvent::class.java))
        }
    }

    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_PAYMENT_FAILED],
        groupId = "order-payment-failed",
    )
    fun onPaymentFailed(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("order-payment-failed", eventId) {
            orderService.handlePaymentFailed(objectMapper.readValue(payload, OrderPaymentFailedEvent::class.java))
        }
    }

    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_PAYMENT_COMPENSATED],
        groupId = "order-payment-compensated",
    )
    fun onPaymentCompensated(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("order-payment-compensated", eventId) {
            orderService.handlePaymentCompensated(objectMapper.readValue(payload, OrderPaymentCompensatedEvent::class.java))
        }
    }

    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_PAYMENT_COMPENSATION_FAILED],
        groupId = "order-payment-compensation-failed",
    )
    fun onPaymentCompensationFailed(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("order-payment-compensation-failed", eventId) {
            orderService.handlePaymentCompensationFailed(
                objectMapper.readValue(payload, OrderPaymentCompensationFailedEvent::class.java),
            )
        }
    }

    private fun consume(consumerName: String, eventId: String, action: () -> Unit) {
        if (consumedEventRepository.existsById(processedEventId(consumerName, eventId))) {
            return
        }

        action()
        consumedEventRepository.save(
            ConsumedEventJpaEntity(
                id = processedEventId(consumerName, eventId),
                consumerName = consumerName,
                eventId = eventId,
            ),
        )
    }

    private fun processedEventId(consumerName: String, eventId: String): String {
        return "$consumerName:$eventId"
    }
}
