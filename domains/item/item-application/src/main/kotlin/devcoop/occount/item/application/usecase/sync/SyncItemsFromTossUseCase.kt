package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.exception.ItemConcurrentUpdateException
import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.output.TossItemPayload
import devcoop.occount.item.application.output.TossItemPort
import devcoop.occount.item.domain.item.ItemInfo
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class SyncItemsFromTossUseCase(
    private val itemRepository: ItemRepository,
    private val tossItemPort: TossItemPort,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun sync() {
        val externalItems = tossItemPort.getItems()
        if (externalItems.isEmpty()) {
            return
        }

        val existing = itemRepository.findAllByItemIds(
            externalItems.map(TossItemPayload::itemId),
        )
            .associateBy { it.getItemId() }

        val newItems = mutableListOf<TossItemPayload>()
        val changedCatalogs = mutableListOf<Pair<Long, ItemInfo>>()

        externalItems.forEach { payload ->
            val current = existing[payload.itemId]
            val itemInfo = payload.toItemInfo()

            when {
                current == null -> newItems += payload
                current.hasSameCatalog(itemInfo) -> Unit
                else -> changedCatalogs += payload.itemId to itemInfo
            }
        }

        if (newItems.isNotEmpty()) {
            transactionTemplate.executeWithoutResult {
                itemRepository.saveCatalogs(newItems.map(TossItemPayload::toItem))
            }
        }

        changedCatalogs.forEach { (itemId, itemInfo) ->
            syncCatalogWithRetry(itemId, itemInfo)
        }
    }

    private fun syncCatalogWithRetry(itemId: Long, itemInfo: ItemInfo) {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                transactionTemplate.executeWithoutResult {
                    val current = itemRepository.findById(itemId)

                    if (current == null) {
                        itemRepository.saveCatalog(
                            TossItemPayload(
                                itemId = itemId,
                                name = itemInfo.name(),
                                category = itemInfo.category(),
                                price = itemInfo.price(),
                                barcode = itemInfo.barcode(),
                            ).toItem(),
                        )
                        return@executeWithoutResult
                    }

                    if (current.hasSameCatalog(itemInfo)) {
                        return@executeWithoutResult
                    }

                    itemRepository.saveCatalog(current.update(itemInfo))
                }
                return
            } catch (ex: OptimisticLockingFailureException) {
                if (attempt == MAX_RETRY_COUNT - 1) {
                    throw ItemConcurrentUpdateException()
                }
            }
        }
    }

    private companion object {
        const val MAX_RETRY_COUNT = 3
    }
}
