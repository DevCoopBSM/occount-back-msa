package devcoop.occount.item.application.usecase.order

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderItemPayload
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompletedEvent
import devcoop.occount.core.common.event.OrderStockFailedEvent
import devcoop.occount.core.common.exception.BusinessBaseException
import devcoop.occount.item.application.exception.DuplicateEventException
import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.domain.item.Item
import devcoop.occount.item.domain.item.ItemNotFoundException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class ProcessOrderRequestedUseCase(
    private val itemRepository: ItemRepository,
    private val eventPublisher: EventPublisher,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun process(event: OrderRequestedEvent, recordConsumption: () -> Unit) {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                transactionTemplate.executeWithoutResult {
                    recordConsumption()
                    processOnce(event)
                }
                return
            } catch (_: DuplicateEventException) {
                return
            } catch (ex: OptimisticLockingFailureException) {
                if (attempt == MAX_RETRY_COUNT - 1) {
                    markFailed(event, ErrorMessage.ITEM_CONCURRENT_UPDATE.message, recordConsumption)
                    return
                }
            }
        }
    }

    private fun processOnce(event: OrderRequestedEvent) {
        val requestedItems = aggregateRequestedItems(event)
        try {
            val itemsById = itemRepository.findAllByItemIds(requestedItems.keys.toList())
                .associateBy(Item::getItemId)

            val updatedItems = mutableListOf<Item>()
            val confirmedItems = requestedItems.map { (itemId, quantity) ->
                val item = itemsById[itemId] ?: throw ItemNotFoundException()
                if (!item.isActive()) throw ItemNotFoundException()

                val updatedItem = item.decreaseQuantity(quantity)
                updatedItems += updatedItem

                OrderItemPayload(
                    itemId = itemId,
                    itemName = item.getName(),
                    itemPrice = item.getPrice(),
                    quantity = quantity,
                    totalPrice = item.getPrice() * quantity,
                )
            }

            itemRepository.saveStocks(updatedItems)
            eventPublisher.publish(
                topic = DomainTopics.ORDER_STOCK_COMPLETED,
                key = event.orderId,
                eventType = DomainEventTypes.ORDER_STOCK_COMPLETED,
                payload = OrderStockCompletedEvent(
                    orderId = event.orderId,
                    items = confirmedItems,
                    totalAmount = confirmedItems.sumOf(OrderItemPayload::totalPrice),
                ),
            )
        } catch (ex: BusinessBaseException) {
            publishFailed(event.orderId, ex.message ?: ErrorMessage.INTERNAL_SERVER_ERROR.message)
        }
    }

    private fun aggregateRequestedItems(event: OrderRequestedEvent): LinkedHashMap<Long, Int> {
        val quantitiesByItemId = linkedMapOf<Long, Int>()
        event.items.forEach { item ->
            quantitiesByItemId[item.itemId] = (quantitiesByItemId[item.itemId] ?: 0) + item.quantity
        }
        return quantitiesByItemId
    }

    private fun markFailed(event: OrderRequestedEvent, reason: String, recordConsumption: () -> Unit) {
        transactionTemplate.executeWithoutResult {
            try {
                recordConsumption()
            } catch (_: DuplicateEventException) {
                return@executeWithoutResult
            }
            publishFailed(event.orderId, reason)
        }
    }

    private fun publishFailed(orderId: String, reason: String) {
        eventPublisher.publish(
            topic = DomainTopics.ORDER_STOCK_FAILED,
            key = orderId,
            eventType = DomainEventTypes.ORDER_STOCK_FAILED,
            payload = OrderStockFailedEvent(
                orderId = orderId,
                reason = reason,
            ),
        )
    }

    private companion object {
        const val MAX_RETRY_COUNT = 3
    }
}
