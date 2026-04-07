package devcoop.occount.payment.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.payment.application.usecase.payment.CompensateOrderPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.ProcessOrderPaymentUseCase
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class OrderPaymentEventProcessor(
    private val processOrderPaymentUseCase: ProcessOrderPaymentUseCase,
    private val compensateOrderPaymentUseCase: CompensateOrderPaymentUseCase,
    private val consumedEventRepository: ConsumedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_REQUESTED],
        groupId = "payment-order-requested",
    )
    fun onOrderRequested(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("payment-order-requested", eventId) {
            processOrderPaymentUseCase.process(
                objectMapper.readValue(payload, OrderRequestedEvent::class.java),
            )
        }
    }

    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_PAYMENT_COMPENSATION_REQUESTED],
        groupId = "payment-order-compensation",
    )
    fun onOrderPaymentCompensationRequested(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("payment-order-compensation", eventId) {
            compensateOrderPaymentUseCase.compensate(
                objectMapper.readValue(payload, OrderPaymentCompensationRequestedEvent::class.java),
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
