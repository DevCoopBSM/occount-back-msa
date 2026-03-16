package devcoop.occount.product.application.item

import devcoop.occount.product.domain.item.Category

data class ItemUpdateRequest(
    val name: String,
    val category: Category,
    val price: Int,
    val barcode: String? = null,
    val quantity: Int,
)
