package devcoop.occount.item.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.item.application.exception.DuplicateEventException
import devcoop.occount.item.application.usecase.order.ProcessOrderRequestedUseCase
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class OrderRequestedEventListener(
    private val processOrderRequestedUseCase: ProcessOrderRequestedUseCase,
    private val consumedEventRepository: ConsumedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    @KafkaListener(topics = [DomainTopics.ORDER_REQUESTED], groupId = "order-requested")
    fun onOrderRequested(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        processOrderRequestedUseCase.process(
            event = objectMapper.readValue(payload, OrderRequestedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-requested", eventId) },
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
