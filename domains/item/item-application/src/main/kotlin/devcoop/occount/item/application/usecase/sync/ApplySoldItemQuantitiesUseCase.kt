package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.output.SoldItemPayload
import devcoop.occount.item.application.output.TossItemPort
import devcoop.occount.item.domain.item.Item
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ApplySoldItemQuantitiesUseCase(
    private val itemRepository: ItemRepository,
    private val tossItemPort: TossItemPort,
    private val syncItemsFromTossUseCase: SyncItemsFromTossUseCase,
) {
    @Transactional
    fun apply() {
        val soldItems = tossItemPort.getSoldItems()
        if (soldItems.isEmpty()) {
            return
        }

        val soldItemsByName = soldItems.associateBy(SoldItemPayload::name)
        val soldItemNames = soldItems.map(SoldItemPayload::name)

        if (itemRepository.existsItemByNameIsNotIn(soldItemNames)) {
            syncItemsFromTossUseCase.sync()
        }

        val updatedItems = itemRepository.findAllByNameIn(soldItemNames).map { item ->
            val soldItem = soldItemsByName[item.getName()]
            if (soldItem == null) {
                item
            } else {
                item.decreaseQuantity(soldItem.quantity)
            }
        }

        itemRepository.saveAll(updatedItems)
    }
}
