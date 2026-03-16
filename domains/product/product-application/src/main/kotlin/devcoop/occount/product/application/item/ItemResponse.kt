package devcoop.occount.product.application.item

import devcoop.occount.product.domain.item.Category

data class ItemResponse(
    val itemId: Long,
    val name: String,
    val category: Category,
    val price: Int,
    val barcode: String? = null,
)
