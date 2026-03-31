package devcoop.occount.item.infrastructure.client.toss

import devcoop.occount.item.domain.item.Category

data class TossItemListResponse(
    val items: List<TossItemResponse>,
)

data class TossItemResponse(
    val itemId: Long,
    val name: String,
    val category: Category,
    val price: Int,
    val barcode: String? = null,
)

data class TossSoldItemListResponse(
    val soldItems: List<TossSoldItemResponse>,
)

data class TossSoldItemResponse(
    val name: String,
    val quantity: Int,
)
