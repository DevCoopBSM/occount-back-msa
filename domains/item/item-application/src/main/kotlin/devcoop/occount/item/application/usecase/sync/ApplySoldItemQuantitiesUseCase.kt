package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.output.SoldItemPayload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ApplySoldItemQuantitiesUseCase(
    private val itemRepository: ItemRepository,
) {
    @Transactional
    fun apply(soldItems: List<SoldItemPayload>) {
        val soldItemsByName = soldItems.associateBy(SoldItemPayload::name)
        val soldItemNames = soldItems.map(SoldItemPayload::name)

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
