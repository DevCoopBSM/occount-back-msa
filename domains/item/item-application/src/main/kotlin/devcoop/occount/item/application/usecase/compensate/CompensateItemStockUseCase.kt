package devcoop.occount.item.application.usecase.compensate

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.ItemStockCompensatedEvent
import devcoop.occount.core.common.event.ItemStockCompensationFailedEvent
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
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
class CompensateItemStockUseCase(
    private val itemRepository: ItemRepository,
    private val eventPublisher: EventPublisher,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun compensate(event: OrderStockCompensationRequestedEvent, recordConsumption: () -> Unit) {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                transactionTemplate.executeWithoutResult {
                    recordConsumption()
                    compensateOnce(event)
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

    private fun compensateOnce(event: OrderStockCompensationRequestedEvent) {
        val requestedItems = aggregateRequestedItems(event)
        try {
            val itemsById = itemRepository.findAllByItemIds(requestedItems.keys.toList())
                .associateBy(Item::getItemId)

            val restoredItems = requestedItems.map { (itemId, quantity) ->
                val item = itemsById[itemId] ?: throw ItemNotFoundException()
                item.increaseQuantity(quantity)
            }

            itemRepository.saveStocks(restoredItems)
            eventPublisher.publish(
                topic = DomainTopics.ITEM_STOCK_COMPENSATED,
                key = event.orderId,
                eventType = DomainEventTypes.ITEM_STOCK_COMPENSATED,
                payload = ItemStockCompensatedEvent(orderId = event.orderId),
            )
        } catch (ex: BusinessBaseException) {
            publishFailed(event.orderId, ex.message ?: ErrorMessage.INTERNAL_SERVER_ERROR.message)
        }
    }

    private fun aggregateRequestedItems(event: OrderStockCompensationRequestedEvent): LinkedHashMap<Long, Int> {
        val quantitiesByItemId = linkedMapOf<Long, Int>()
        event.items.forEach { item ->
            quantitiesByItemId[item.itemId] = (quantitiesByItemId[item.itemId] ?: 0) + item.quantity
        }
        return quantitiesByItemId
    }

    private fun markFailed(
        event: OrderStockCompensationRequestedEvent,
        reason: String,
        recordConsumption: () -> Unit,
    ) {
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
            topic = DomainTopics.ITEM_STOCK_COMPENSATION_FAILED,
            key = orderId,
            eventType = DomainEventTypes.ITEM_STOCK_COMPENSATION_FAILED,
            payload = ItemStockCompensationFailedEvent(
                orderId = orderId,
                reason = reason,
            ),
        )
    }

    private companion object {
        const val MAX_RETRY_COUNT = 3
    }
}
