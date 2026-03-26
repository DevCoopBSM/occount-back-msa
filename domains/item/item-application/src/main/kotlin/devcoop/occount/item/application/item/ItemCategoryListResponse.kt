package devcoop.occount.item.application.item

import devcoop.occount.item.domain.item.Category

data class ItemCategoryListResponse(
    val itemCategories: List<Category>,
)
