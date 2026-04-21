package devcoop.occount.item.application.usecase.create

import devcoop.occount.item.domain.item.Category

data class CreateItemRequest(
    val name: String,
    val category: Category,
    val price: Int,
    val barcode: String? = null,
    val quantity: Int,
)
