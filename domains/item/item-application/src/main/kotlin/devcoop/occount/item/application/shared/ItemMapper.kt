package devcoop.occount.item.application.shared

import devcoop.occount.item.domain.item.Item

object ItemMapper {
    fun toResponse(item: Item): ItemResponse {
        return ItemResponse(
            itemId = item.getItemId(),
            name = item.getName(),
            category = item.getCategory(),
            price = item.getPrice(),
            quantity = item.getQuantity(),
            barcode = item.getBarcode(),
        )
    }

    fun toLookupResponse(item: Item): ItemLookupResponse {
        return ItemLookupResponse(
            itemId = item.getItemId(),
            name = item.getName(),
            barcode = item.getBarcode(),
            price = item.getPrice(),
            isActive = item.isActive(),
        )
    }
}
