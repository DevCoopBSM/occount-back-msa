package devcoop.occount.item.application.item

import devcoop.occount.item.domain.item.Category

data class ItemResponse(
    val itemId: Long,
    val name: String,
    val category: Category,
    val price: Int,
    val barcode: String? = null,
)
