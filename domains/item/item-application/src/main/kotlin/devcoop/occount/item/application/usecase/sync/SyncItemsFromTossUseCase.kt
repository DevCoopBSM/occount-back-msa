package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.output.TossItemPayload
import devcoop.occount.item.application.output.TossItemPort
import devcoop.occount.item.domain.item.Item
import devcoop.occount.item.domain.item.ItemInfo
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

        val existing = itemRepository.findAll()
            .associateBy { it.getItemId() }

        val newItems = externalItems.map { payload ->
            val current = existing[payload.itemId]

            current?.update(payload.toItemInfo())
                ?: payload.toItem()
        }

        itemRepository.saveAll(newItems)
    }
}
