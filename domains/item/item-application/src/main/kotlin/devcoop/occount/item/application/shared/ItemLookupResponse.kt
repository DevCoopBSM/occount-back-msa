package devcoop.occount.item.application.shared

import devcoop.occount.item.domain.item.Category

data class ItemLookupResponse(
    val itemId: Long,
    val name: String,
    val category: Category,
    val barcode: String?,
    val price: Int,
    val isActive: Boolean = true,
)
