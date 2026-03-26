package devcoop.occount.item.application.item

import devcoop.occount.item.domain.item.Category
import devcoop.occount.item.domain.item.ItemNotFoundException
import org.springframework.stereotype.Service

@Service
class ItemService(
    private val itemRepository: ItemRepository,
    private val tossPort: TossPort,
) {
    fun getAllItems(): ItemListResponse {
        return ItemListResponse(
            items = itemRepository.findAll().map(ItemMapper::toResponse),
        )
    }

    fun getItemCategories(): ItemCategoryListResponse {
        return ItemCategoryListResponse(Category.entries)
    }

    fun getItemsWithoutBarcode(): ItemLookupListResponse {
        return ItemLookupListResponse(
            items = itemRepository.findAllWithoutBarcode().map(ItemMapper::toLookupResponse),
        )
    }

    fun getItemByBarcode(barcode: String): ItemLookupResponse {
        val item = itemRepository.findByBarcode(barcode)
            ?: throw ItemNotFoundException()

        return ItemMapper.toLookupResponse(item)
    }

    fun updateItem(id: Long, request: ItemUpdateRequest): ItemResponse {
        val item = itemRepository.findById(id)
            ?: throw ItemNotFoundException()

        val updatedItem = ItemMapper.toEntity(item.getItemId(), request)
        itemRepository.save(updatedItem)

        return ItemMapper.toResponse(updatedItem)
    }

    fun deleteItem(id: Long) {
        val item = itemRepository.findById(id)
            ?: throw ItemNotFoundException()

        item.deactivate()
        itemRepository.save(item)
    }

    fun syncItemsFromToss() {
        val externalItems = tossPort.getItemList().items
        val externalItemsById = externalItems.associateBy { it.itemId }
        val existingIds = itemRepository.findAllIds()

        val newItems = externalItemsById
            .filterKeys { it !in existingIds }
            .values
            .map(ItemMapper::toEntity)

        if (newItems.isNotEmpty()) {
            itemRepository.saveAll(newItems)
        }
    }

    fun applySoldItemQuantities() {
        val soldItems = tossPort.getSoldItems().soldItems
        if (soldItems.isEmpty()) {
            return
        }

        val soldItemsByName = soldItems.associateBy { it.name }
        val soldItemNames = soldItems.map { it.name }

        if (itemRepository.existsItemByNameIsNotIn(soldItemNames)) {
            syncItemsFromToss()
        }

        val items = itemRepository.findAllByNameIn(soldItemNames)
        items.forEach { item ->
            soldItemsByName[item.getName()]?.let { soldItem ->
                item.decreaseQuantity(soldItem.quantity)
            }
        }

        itemRepository.saveAll(items)
    }
}
