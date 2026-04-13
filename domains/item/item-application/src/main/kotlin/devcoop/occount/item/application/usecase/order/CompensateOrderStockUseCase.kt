package devcoop.occount.item.application.usecase.order

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderStockCompensatedEvent
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompensationFailedEvent
import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.domain.item.ItemNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompensateOrderStockUseCase(
    private val itemRepository: ItemRepository,
    private val eventPublisher: EventPublisher,
) {
    @Transactional
    fun compensate(event: OrderStockCompensationRequestedEvent) {
        try {
            val itemsById = itemRepository.findAllByItemIds(
                event.items.map { it.itemId },
            ).associateBy { it.getItemId() }

            val updatedItems = event.items.map { payload ->
                val item = itemsById[payload.itemId] ?: throw ItemNotFoundException()
                item.updateQuantity(item.getQuantity() + payload.quantity)
            }

            itemRepository.saveStocks(updatedItems)

            eventPublisher.publish(
                topic = DomainTopics.ORDER_STOCK_COMPENSATED,
                key = event.orderId,
                eventType = DomainEventTypes.ORDER_STOCK_COMPENSATED,
                payload = OrderStockCompensatedEvent(orderId = event.orderId),
            )
        } catch (ex: Exception) {
            eventPublisher.publish(
                topic = DomainTopics.ORDER_STOCK_COMPENSATION_FAILED,
                key = event.orderId,
                eventType = DomainEventTypes.ORDER_STOCK_COMPENSATION_FAILED,
                payload = OrderStockCompensationFailedEvent(
                    orderId = event.orderId,
                    reason = ex.message ?: "Stock compensation failed",
                ),
            )
        }
    }
}
