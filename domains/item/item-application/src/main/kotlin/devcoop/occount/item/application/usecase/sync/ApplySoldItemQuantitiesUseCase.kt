package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.exception.ItemNotSynchronizedException
import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.output.SoldItemPayload
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class ApplySoldItemQuantitiesUseCase(
    private val itemRepository: ItemRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun apply(soldItems: List<SoldItemPayload>) {
        val aggregatedSoldItems = soldItems
            .groupBy(SoldItemPayload::name)
            .map { (name, payloads) ->
                SoldItemPayload(
                    name = name,
                    quantity = payloads.sumOf(SoldItemPayload::quantity),
                )
            }
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                transactionTemplate.executeWithoutResult {
                    applyOnce(aggregatedSoldItems)
                }
                return
            } catch (ex: OptimisticLockingFailureException) {
                if (attempt == MAX_RETRY_COUNT - 1) {
                    throw ex
                }
            }
        }
    }

    private fun applyOnce(soldItems: List<SoldItemPayload>) {
        val soldItemsByName = soldItems.associateBy(SoldItemPayload::name)
        val soldItemNames = soldItemsByName.keys.toList()
        val items = itemRepository.findAllByNameIn(soldItemNames)
        val foundNames = items.map { it.getName() }.toSet()

        if (foundNames.size != soldItemNames.size) {
            throw ItemNotSynchronizedException()
        }

        val updatedItems = items.map { item ->
            item.decreaseQuantity(
                soldItemsByName.getValue(item.getName()).quantity,
            )
        }

        itemRepository.saveAll(updatedItems)
    }

    private companion object {
        const val MAX_RETRY_COUNT = 3
    }
}
