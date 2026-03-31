package devcoop.occount.item.application.usecase.update

import devcoop.occount.item.application.exception.ItemConcurrentUpdateException
import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.shared.ItemMapper
import devcoop.occount.item.application.shared.ItemResponse
import devcoop.occount.item.domain.item.ItemInfo
import devcoop.occount.item.domain.item.ItemNotFoundException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class UpdateItemUseCase(
    private val itemRepository: ItemRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun update(id: Long, request: ItemUpdateRequest): ItemResponse {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                return transactionTemplate.execute {
                    val item = itemRepository.findById(id)
                        ?: throw ItemNotFoundException()

                    val requestedItemInfo = ItemInfo(
                        name = request.name,
                        category = request.category,
                        price = request.price,
                        barcode = request.barcode,
                    )
                    val hasSameCatalog = item.hasSameCatalog(requestedItemInfo)
                    val hasSameQuantity = item.hasSameQuantity(request.quantity)

                    when {
                        hasSameCatalog && hasSameQuantity -> item
                        hasSameCatalog -> itemRepository.saveStock(
                            item.updateQuantity(request.quantity),
                        )

                        hasSameQuantity -> itemRepository.saveCatalog(
                            item.update(requestedItemInfo),
                        )

                        else -> itemRepository.save(
                            item.update(
                                itemInfo = requestedItemInfo,
                                quantity = request.quantity,
                            ),
                        )
                    }.let(ItemMapper::toResponse)
                }
            } catch (ex: OptimisticLockingFailureException) {
                if (attempt == MAX_RETRY_COUNT - 1) {
                    throw ItemConcurrentUpdateException()
                }
            }
        }

        throw IllegalStateException("Unreachable update retry state")
    }

    private companion object {
        const val MAX_RETRY_COUNT = 3
    }
}
