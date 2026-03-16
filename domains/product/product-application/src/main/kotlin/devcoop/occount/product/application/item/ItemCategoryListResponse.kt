package devcoop.occount.product.application.item

import devcoop.occount.product.domain.item.Category

data class ItemCategoryListResponse(
    val itemCategories: List<Category>,
)
