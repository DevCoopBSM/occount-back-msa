package devcoop.occount.order.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.OrderStockCompensatedEvent
import devcoop.occount.core.common.event.OrderStockCompensationFailedEvent
import devcoop.occount.core.common.event.OrderStockCompletedEvent
import devcoop.occount.core.common.event.OrderStockFailedEvent
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.order.application.usecase.order.event.HandleOrderStockEventUseCase
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
    @KafkaListener(topics = [DomainTopics.ORDER_STOCK_COMPLETED], groupId = "order-stock-completed")
    fun onStockCompleted(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderStockEventUseCase.applyCompletedStock(
            event = objectMapper.readValue(payload, OrderStockCompletedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-stock-completed", eventId) },
        )
    }

    @KafkaListener(topics = [DomainTopics.ORDER_STOCK_FAILED], groupId = "order-stock-failed")
    fun onStockFailed(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderStockEventUseCase.applyFailedStock(
            event = objectMapper.readValue(payload, OrderStockFailedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-stock-failed", eventId) },
        )
    }

    @KafkaListener(topics = [DomainTopics.ORDER_STOCK_COMPENSATED], groupId = "order-stock-compensated")
    fun onStockCompensated(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderStockEventUseCase.applyCompensatedStock(
            event = objectMapper.readValue(payload, OrderStockCompensatedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-stock-compensated", eventId) },
        )
    }

    @KafkaListener(topics = [DomainTopics.ORDER_STOCK_COMPENSATION_FAILED], groupId = "order-stock-compensation-failed")
    fun onStockCompensationFailed(payload: String, @Header(DomainEventHeaders.EVENT_ID) eventId: String) {
        handleOrderStockEventUseCase.applyStockCompensationFailure(
            event = objectMapper.readValue(payload, OrderStockCompensationFailedEvent::class.java),
            recordConsumption = { saveConsumedEvent("order-stock-compensation-failed", eventId) },
        )
    }

    private fun saveConsumedEvent(consumerName: String, eventId: String) {
        consumedEventRepository.save(
            ConsumedEventJpaEntity(
                id = "$consumerName:$eventId",
                consumerName = consumerName,
                eventId = eventId,
            ),
        )
    }
}
