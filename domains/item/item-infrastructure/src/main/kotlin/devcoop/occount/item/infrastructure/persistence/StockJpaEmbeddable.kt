package devcoop.occount.item.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class StockJpaEmbeddable(
    @field:Column(name = "quantity", nullable = false)
    private var quantity: Int = 0,
) {
    fun getQuantity() = quantity
}
