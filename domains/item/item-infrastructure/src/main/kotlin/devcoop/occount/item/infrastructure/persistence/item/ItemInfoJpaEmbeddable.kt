package devcoop.occount.item.infrastructure.persistence.item

import devcoop.occount.item.domain.item.Category
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
class ItemInfoJpaEmbeddable(
    @field:Column(name = "name", nullable = false, unique = true)
    private var name: String,
    @Enumerated(EnumType.STRING)
    @field:Column(name = "category", nullable = false)
    private var category: Category,
    @field:Column(name = "price", nullable = false)
    private var price: Int,
    @field:Column(name = "barcode")
    private var barcode: String? = null,
) {
    fun getName() = name
    fun getCategory() = category
    fun getPrice() = price
    fun getBarcode() = barcode
}
