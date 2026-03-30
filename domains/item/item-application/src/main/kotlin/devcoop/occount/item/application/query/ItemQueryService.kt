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
}
