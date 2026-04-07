package devcoop.occount.item.infrastructure.event

import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.item.application.usecase.order.CompensateOrderStockUseCase
import devcoop.occount.item.application.usecase.order.DeductStockForOrderUseCase
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class OrderStockEventProcessor(
    private val deductStockForOrderUseCase: DeductStockForOrderUseCase,
    private val compensateOrderStockUseCase: CompensateOrderStockUseCase,
    private val consumedEventRepository: ConsumedEventRepository,
    private val objectMapper: ObjectMapper,
) {
    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_REQUESTED],
        groupId = "item-order-requested",
    )
    fun onOrderRequested(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("item-order-requested", eventId) {
            deductStockForOrderUseCase.deduct(
                objectMapper.readValue(payload, OrderRequestedEvent::class.java),
            )
        }
    }

    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_STOCK_COMPENSATION_REQUESTED],
        groupId = "item-order-compensation",
    )
    fun onOrderStockCompensationRequested(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        consume("item-order-compensation", eventId) {
            compensateOrderStockUseCase.compensate(
                objectMapper.readValue(payload, OrderStockCompensationRequestedEvent::class.java),
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
