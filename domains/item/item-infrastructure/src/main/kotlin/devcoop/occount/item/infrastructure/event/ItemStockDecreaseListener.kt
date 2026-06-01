package devcoop.occount.item.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.db.outbox.IdempotencyTracker
import devcoop.occount.item.application.exception.DuplicateEventException
import devcoop.occount.item.application.usecase.decrease.DecreaseItemStockUseCase
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ItemStockDecreaseListener(
    private val decreaseItemStockUseCase: DecreaseItemStockUseCase,
    consumedEventRepository: ConsumedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    private val idempotency = IdempotencyTracker(CONSUMER_NAME, consumedEventRepository)

    @KafkaListener(topics = [DomainTopics.ORDER_REQUESTED], groupId = CONSUMER_NAME)
    fun decreaseItemStock(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        decreaseItemStockUseCase.decrease(
            event = objectMapper.readValue(payload, OrderRequestedEvent::class.java),
            recordConsumption = { idempotency.recordOrThrowOnDuplicate(eventId) { throw DuplicateEventException() } },
        )
    }

    companion object {
        private const val CONSUMER_NAME = "order-item-decrease"
    }
}
