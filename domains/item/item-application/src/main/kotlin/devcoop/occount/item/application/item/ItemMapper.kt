package devcoop.occount.item.application.item

import devcoop.occount.item.domain.item.Item
import devcoop.occount.item.domain.item.ItemInfo
import devcoop.occount.item.domain.item.Stock

object ItemMapper {
    fun toResponse(item: Item): ItemResponse {
        return ItemResponse(
            itemId = item.getItemId(),
            name = item.getName(),
            category = item.getCategory(),
            price = item.getPrice(),
            barcode = item.getBarcode(),
        )
    }

    fun toLookupResponse(item: Item): ItemLookupResponse {
        return ItemLookupResponse(
            itemId = item.getItemId(),
            name = item.getName(),
            barcode = item.getBarcode(),
            price = item.getPrice(),
        )
    }

    fun toEntity(response: ItemResponse): Item {
        return Item(
            itemId = response.itemId,
            itemInfo = ItemInfo(
                name = response.name,
                category = response.category,
                price = response.price,
                barcode = response.barcode,
            ),
        )
    }

    fun toEntity(id: Long, request: ItemUpdateRequest): Item {
        return Item(
            itemId = id,
            itemInfo = ItemInfo(
                name = request.name,
                category = request.category,
                price = request.price,
                barcode = request.barcode,
            ),
            stock = Stock(quantity = request.quantity),
        )
    }
}
