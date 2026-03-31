package devcoop.occount.item.application.output

import devcoop.occount.item.domain.item.Category
import devcoop.occount.item.domain.item.Item
import devcoop.occount.item.domain.item.ItemInfo

data class TossItemPayload(
    val itemId: Long,
    val name: String,
    val category: Category,
    val price: Int,
    val barcode: String? = null,
) {
    fun toItem(): Item {
        return Item(
            itemId = itemId,
            itemInfo = ItemInfo(
                name = name,
                category = category,
                price = price,
                barcode = barcode,
            ),
        )
    }

    fun toItemInfo(): ItemInfo {
        return ItemInfo(
            name = name,
            category = category,
            price = price,
            barcode = barcode,
        )
    }
}
