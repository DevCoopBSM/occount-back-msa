package devcoop.occount.item.infrastructure.persistence.item

import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "item")
class ItemJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:Column(name = "item_id")
    private var itemId: Long = 0L,
    @Embedded
    private var itemInfo: ItemInfoJpaEmbeddable,
    @Embedded
    private var stock: StockJpaEmbeddable = StockJpaEmbeddable(),
    @field:Column(name = "is_active", nullable = false)
    private var isActive: Boolean = true,
    @field:Column(name = "catalog_version", nullable = false)
    private var catalogVersion: Long = 0L,
    @field:Column(name = "stock_version", nullable = false)
    private var stockVersion: Long = 0L,
) {
    fun getItemId() = itemId
    fun getItemInfo() = itemInfo
    fun getStock() = stock
    fun isActive() = isActive
    fun getCatalogVersion() = catalogVersion
    fun getStockVersion() = stockVersion
}
