package devcoop.occount.item.application.item

import devcoop.occount.item.domain.item.Category

data class ItemUpdateRequest(
    val name: String,
    val category: Category,
    val price: Int,
    val barcode: String? = null,
    val quantity: Int,
)
