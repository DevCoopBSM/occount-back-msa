package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.output.SoldItemPayload
import devcoop.occount.item.application.output.TossItemPort
import org.springframework.stereotype.Service

@Service
class StockSyncFacade(
    private val itemRepository: ItemRepository,
    private val tossItemPort: TossItemPort,
    private val syncItemsFromTossUseCase: SyncItemsFromTossUseCase,
    private val applySoldItemQuantitiesUseCase: ApplySoldItemQuantitiesUseCase,
) {
    fun apply() {
        val soldItems = tossItemPort.getSoldItems()
        if (soldItems.isEmpty()) {
            return
        }

        val soldItemNames = soldItems.map(SoldItemPayload::name).toSet()
        val existingSoldItemNames = itemRepository.findAllByNameIn(soldItemNames.toList())
            .map { item -> item.getName() }

        if (soldItemNames.any { it !in existingSoldItemNames }) {
            syncItemsFromTossUseCase.sync()
        }

        applySoldItemQuantitiesUseCase.apply(soldItems)
    }
}
