package devcoop.occount.order.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.ItemStockCompensatedEvent
import devcoop.occount.core.common.event.ItemStockCompensationFailedEvent
import devcoop.occount.core.common.event.ItemStockDecreasedEvent
import devcoop.occount.core.common.event.ItemStockDecreaseFailedEvent
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.db.outbox.IdempotencyTracker
import devcoop.occount.order.application.exception.DuplicateEventException
import devcoop.occount.order.application.usecase.order.event.HandleOrderStockEventUseCase
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class OrderStockEventListener(
    private val handleOrderStockEventUseCase: HandleOrderStockEventUseCase,
    consumedEventRepository: ConsumedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    private val idempotency = IdempotencyTracker(CONSUMER_NAME, consumedEventRepository)

    @KafkaListener(topics = [DomainTopics.ITEM_EVENTS], groupId = CONSUMER_NAME)
    fun handleStockEvent(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
        @Header(DomainEventHeaders.EVENT_TYPE) eventType: String,
    ) {
        when (eventType) {
            DomainEventTypes.ITEM_STOCK_DECREASED -> handleOrderStockEventUseCase.applyCompletedStock(
                event = objectMapper.readValue(payload, ItemStockDecreasedEvent::class.java),
                recordConsumption = { recordConsumption(eventId) },
            )
            DomainEventTypes.ITEM_STOCK_DECREASE_FAILED -> handleOrderStockEventUseCase.applyFailedStock(
                event = objectMapper.readValue(payload, ItemStockDecreaseFailedEvent::class.java),
                recordConsumption = { recordConsumption(eventId) },
            )
            DomainEventTypes.ITEM_STOCK_COMPENSATED -> handleOrderStockEventUseCase.applyCompensatedStock(
                event = objectMapper.readValue(payload, ItemStockCompensatedEvent::class.java),
                recordConsumption = { recordConsumption(eventId) },
            )
            DomainEventTypes.ITEM_STOCK_COMPENSATION_FAILED -> handleOrderStockEventUseCase.applyStockCompensationFailure(
                event = objectMapper.readValue(payload, ItemStockCompensationFailedEvent::class.java),
                recordConsumption = { recordConsumption(eventId) },
            )
            else -> log.warn("알 수 없는 재고 이벤트 eventType={} eventId={}", eventType, eventId)
        }
    }

    private fun recordConsumption(eventId: String) {
        idempotency.recordOrThrowOnDuplicate(eventId) { throw DuplicateEventException() }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderStockEventListener::class.java)
        private const val CONSUMER_NAME = "item-event-stock-v1"
    }
}
