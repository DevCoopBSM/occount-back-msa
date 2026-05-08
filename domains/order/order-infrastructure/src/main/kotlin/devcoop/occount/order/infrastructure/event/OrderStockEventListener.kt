package devcoop.occount.order.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.ItemStockCompensatedEvent
import devcoop.occount.core.common.event.ItemStockCompensationFailedEvent
import devcoop.occount.core.common.event.ItemStockDecreasedEvent
import devcoop.occount.core.common.event.ItemStockDecreaseFailedEvent
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.order.application.exception.DuplicateEventException
import devcoop.occount.order.application.usecase.order.event.HandleOrderStockEventUseCase
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class OrderStockEventListener(
    private val handleOrderStockEventUseCase: HandleOrderStockEventUseCase,
    private val consumedEventRepository: ConsumedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    @KafkaListener(topics = [DomainTopics.ITEM_STOCK_DECREASED], groupId = "order-stock-completed")
    fun onStockCompleted(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderStockEventUseCase.applyCompletedStock(
            event = objectMapper.readValue(payload, ItemStockDecreasedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-stock-completed", eventId) },
        )
    }

    @KafkaListener(topics = [DomainTopics.ITEM_STOCK_DECREASE_FAILED], groupId = "order-stock-failed")
    fun onStockFailed(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderStockEventUseCase.applyFailedStock(
            event = objectMapper.readValue(payload, ItemStockDecreaseFailedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-stock-failed", eventId) },
        )
    }

    @KafkaListener(topics = [DomainTopics.ITEM_STOCK_COMPENSATED], groupId = "order-stock-compensated")
    fun onStockCompensated(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderStockEventUseCase.applyCompensatedStock(
            event = objectMapper.readValue(payload, ItemStockCompensatedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-stock-compensated", eventId) },
        )
    }

    @KafkaListener(topics = [DomainTopics.ITEM_STOCK_COMPENSATION_FAILED], groupId = "order-stock-compensation-failed")
    fun onStockCompensationFailed(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderStockEventUseCase.applyStockCompensationFailure(
            event = objectMapper.readValue(payload, ItemStockCompensationFailedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-stock-compensation-failed", eventId) },
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
