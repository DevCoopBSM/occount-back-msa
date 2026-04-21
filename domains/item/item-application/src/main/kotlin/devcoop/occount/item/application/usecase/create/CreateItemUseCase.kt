package devcoop.occount.item.application.usecase.create

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.shared.ItemMapper
import devcoop.occount.item.application.shared.ItemResponse
import devcoop.occount.item.domain.item.Item
import devcoop.occount.item.domain.item.ItemAlreadyExistsException
import devcoop.occount.item.domain.item.ItemInfo
import devcoop.occount.item.domain.item.Stock
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class CreateItemUseCase(
    private val itemRepository: ItemRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun create(request: CreateItemRequest): ItemResponse {
        return try {
            transactionTemplate.execute {
                val item = Item(
                    itemInfo = ItemInfo(
                        name = request.name,
                        category = request.category,
                        price = request.price,
                        barcode = request.barcode,
                    ),
                    stock = Stock(request.quantity),
                )
                itemRepository.save(item)
            }.let(ItemMapper::toResponse)
        } catch (ex: DataIntegrityViolationException) {
            throw ItemAlreadyExistsException()
        }
    }
}
