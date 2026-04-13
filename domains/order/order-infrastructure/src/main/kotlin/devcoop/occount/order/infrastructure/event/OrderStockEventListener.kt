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
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class OrderStockEventListener(
    private val handleOrderStockEventUseCase: HandleOrderStockEventUseCase,
    private val consumedEventRepository: ConsumedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_STOCK_COMPLETED],
        groupId = "order-stock-completed",
    )
    fun onStockCompleted(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("order-stock-completed", eventId) {
            handleOrderStockEventUseCase.applyCompletedStock(
                objectMapper.readValue(payload, OrderStockCompletedEvent::class.java),
            )
        }
    }

    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_STOCK_FAILED],
        groupId = "order-stock-failed",
    )
    fun onStockFailed(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("order-stock-failed", eventId) {
            handleOrderStockEventUseCase.applyFailedStock(
                objectMapper.readValue(payload, OrderStockFailedEvent::class.java),
            )
        }
    }

    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_STOCK_COMPENSATED],
        groupId = "order-stock-compensated",
    )
    fun onStockCompensated(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("order-stock-compensated", eventId) {
            handleOrderStockEventUseCase.applyCompensatedStock(
                objectMapper.readValue(payload, OrderStockCompensatedEvent::class.java),
            )
        }
    }

    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_STOCK_COMPENSATION_FAILED],
        groupId = "order-stock-compensation-failed",
    )
    fun onStockCompensationFailed(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("order-stock-compensation-failed", eventId) {
            handleOrderStockEventUseCase.applyStockCompensationFailure(
                objectMapper.readValue(payload, OrderStockCompensationFailedEvent::class.java),
            )
        }
    }

    private fun consume(consumerName: String, eventId: String, action: () -> Unit) {
        val processedEventId = processedEventId(consumerName, eventId)

        try {
            consumedEventRepository.saveAndFlush(
                ConsumedEventJpaEntity(
                    id = processedEventId,
                    consumerName = consumerName,
                    eventId = eventId,
                ),
            )
        } catch (_: DataIntegrityViolationException) {
            return
        }

        action()
    }

    private fun processedEventId(consumerName: String, eventId: String): String {
        return "$consumerName:$eventId"
    }
}
