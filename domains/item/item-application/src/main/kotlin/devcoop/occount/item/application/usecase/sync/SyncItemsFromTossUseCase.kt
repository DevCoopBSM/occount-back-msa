package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.output.TossItemPayload
import devcoop.occount.item.application.output.TossItemPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SyncItemsFromTossUseCase(
    private val itemRepository: ItemRepository,
    private val tossItemPort: TossItemPort,
) {
    @Transactional
    fun sync() {
        val externalItems = tossItemPort.getItems()
        if (externalItems.isEmpty()) {
            return
        }

        val existing = itemRepository.findAllByItemIds(
            externalItems.map(TossItemPayload::itemId),
        )
            .associateBy { it.getItemId() }

        val upsertItems = externalItems.map { payload ->
            val current = existing[payload.itemId]

            current?.update(payload.toItemInfo())
                ?: payload.toItem()
        }

        itemRepository.saveAll(upsertItems)
    }
}
