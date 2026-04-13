package devcoop.occount.item.application.usecase.order

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompletedEvent
import devcoop.occount.core.common.event.OrderStockFailedEvent
import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.domain.item.ItemNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeductStockForOrderUseCase(
    private val itemRepository: ItemRepository,
    private val eventPublisher: EventPublisher,
) {
    @Transactional
    fun deduct(event: OrderRequestedEvent) {
        try {
            val itemsById = itemRepository.findAllByItemIds(
                event.items.map { it.itemId },
            ).associateBy { it.getItemId() }

            val updatedItems = event.items.map { payload ->
                val item = itemsById[payload.itemId] ?: throw ItemNotFoundException()
                item.decreaseQuantity(payload.quantity)
            }

            itemRepository.saveStocks(updatedItems)

            eventPublisher.publish(
                topic = DomainTopics.ORDER_STOCK_COMPLETED,
                key = event.orderId,
                eventType = DomainEventTypes.ORDER_STOCK_COMPLETED,
                payload = OrderStockCompletedEvent(
                    orderId = event.orderId,
                    itemIds = event.items.map { it.itemId },
                ),
            )
        } catch (ex: Exception) {
            eventPublisher.publish(
                topic = DomainTopics.ORDER_STOCK_FAILED,
                key = event.orderId,
                eventType = DomainEventTypes.ORDER_STOCK_FAILED,
                payload = OrderStockFailedEvent(
                    orderId = event.orderId,
                    itemIds = event.items.map { it.itemId },
                    reason = ex.message ?: "Stock deduction failed",
                ),
            )
        }
    }
}
