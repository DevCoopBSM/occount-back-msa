package devcoop.occount.item.application.query

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.shared.ItemMapper
import devcoop.occount.item.application.shared.ItemLookupResponse
import devcoop.occount.item.domain.item.Category
import devcoop.occount.item.domain.item.ItemNotFoundException
import org.springframework.stereotype.Service

@Service
class ItemQueryService(
    private val itemRepository: ItemRepository,
) {
    fun getAllItems(): ItemListResponse {
        return ItemListResponse(
            items = itemRepository.findAll().map(ItemMapper::toResponse),
        )
    }

    fun getItemCategories(): ItemCategoryListResponse {
        return ItemCategoryListResponse(categories = Category.entries)
    }

    fun getItemsWithoutBarcode(): ItemLookupListResponse {
        return ItemLookupListResponse(
            items = itemRepository.findAllWithoutBarcode().map(ItemMapper::toLookupResponse),
        )
    }

    fun getItemsByIds(ids: List<Long>): ItemLookupListResponse {
        return ItemLookupListResponse(
            items = itemRepository.findAllByItemIds(ids).map(ItemMapper::toLookupResponse),
        )
    }

    fun searchItems(query: String): ItemListResponse {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return ItemListResponse(items = emptyList())
        }

        return ItemListResponse(
            items = itemRepository.searchByName(trimmed).map(ItemMapper::toResponse),
        )
    }

    fun getItemByBarcode(barcode: String): ItemLookupResponse {
        val item = itemRepository.findByBarcode(barcode)
            ?: throw ItemNotFoundException()

        return ItemMapper.toLookupResponse(item)
    }
}
