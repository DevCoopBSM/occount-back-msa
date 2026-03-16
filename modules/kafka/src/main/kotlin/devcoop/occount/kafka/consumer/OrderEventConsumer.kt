package devcoop.occount.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import devcoop.occount.core.common.event.DomainEventHeaders
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.db.outbox.ConsumedEventJpaEntity
import devcoop.occount.db.outbox.ConsumedEventRepository
import devcoop.occount.order.infrastructure.event.OrderRequestedEvent
import devcoop.occount.product.application.item.ItemRepository
import devcoop.occount.product.domain.item.ItemNotFoundException
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
@ConditionalOnBean(ItemRepository::class)
class OrderEventConsumer(
    private val objectMapper: ObjectMapper,
    private val itemRepository: ItemRepository,
    private val consumedEventRepository: ConsumedEventRepository,
) {
    @Transactional
    @KafkaListener(
        topics = [DomainTopics.ORDER_REQUESTED],
        groupId = "product-stock-adjuster-v1",
    )
    fun onOrderRequested(
        payload: String,
        @Header(DomainEventHeaders.EVENT_ID) eventId: String,
    ) {
        val consumerName = "product-stock-adjuster-v1"
        if (isProcessed(consumerName, eventId)) {
            return
        }

        val event = objectMapper.readValue<OrderRequestedEvent>(payload)
        val itemIds = event.orderInfos.map { it.itemId }
        val itemsById = itemRepository.findAllByItemIds(itemIds).associateBy { it.getItemId() }
        event.orderInfos.forEach { orderInfo ->
            val item = itemsById[orderInfo.itemId] ?: throw ItemNotFoundException()
            item.decreaseQuantity(orderInfo.quantity)
        }

        itemRepository.saveAll(itemsById.values.toList())
        markProcessed(consumerName, eventId)
    }

    private fun isProcessed(consumerName: String, eventId: String): Boolean {
        return consumedEventRepository.existsById(processedEventId(consumerName, eventId))
    }

    private fun markProcessed(consumerName: String, eventId: String) {
        consumedEventRepository.save(
            ConsumedEventJpaEntity(
                id = processedEventId(consumerName, eventId),
                consumerName = consumerName,
                eventId = eventId,
                processedAt = Instant.now(),
            ),
        )
    }

    private fun processedEventId(consumerName: String, eventId: String): String {
        return "$consumerName:$eventId"
    }
}
