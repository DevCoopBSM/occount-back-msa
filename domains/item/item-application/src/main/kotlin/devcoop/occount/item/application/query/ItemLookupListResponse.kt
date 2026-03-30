package devcoop.occount.item.application.query

import devcoop.occount.item.application.shared.ItemLookupResponse

data class ItemLookupListResponse(
    val items: List<ItemLookupResponse>,
)
