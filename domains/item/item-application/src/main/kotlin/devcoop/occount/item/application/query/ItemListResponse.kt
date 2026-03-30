package devcoop.occount.item.application.query

import devcoop.occount.item.application.shared.ItemResponse

data class ItemListResponse(
    val items: List<ItemResponse>,
)
