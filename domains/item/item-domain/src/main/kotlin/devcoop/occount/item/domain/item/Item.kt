package devcoop.occount.item.domain.item

data class Item(
    private val itemId: Long = 0L,
    private val itemInfo: ItemInfo,
    private val stock: Stock = Stock(),
    private val isActive: Boolean = true,
    private val catalogVersion: Long = 0L,
    private val stockVersion: Long = 0L,
) {
    fun getItemId() = itemId
    fun getName() = itemInfo.name()
    fun getCategory() = itemInfo.category()
    fun getPrice() = itemInfo.price()
    fun getBarcode() = itemInfo.barcode()
    fun getQuantity() = stock.getQuantity()
    fun isActive() = isActive
    fun getCatalogVersion() = catalogVersion
    fun getStockVersion() = stockVersion

    fun decreaseQuantity(orderQuantity: Int): Item {
        return copy(stock = stock.decreaseQuantity(orderQuantity))
    }

    fun increaseQuantity(quantity: Int): Item {
        return copy(stock = stock.increaseQuantity(quantity))
    }

    fun updateQuantity(quantity: Int): Item {
        return copy(stock = Stock(quantity))
    }

    fun update(itemInfo: ItemInfo, quantity: Int): Item {
        return copy(
            itemInfo = itemInfo,
            stock = Stock(quantity),
        )
    }

    fun update(itemInfo: ItemInfo): Item {
        return copy(
            itemInfo = itemInfo,
            stock = this.stock,
        )
    }

    fun deactivate(): Item {
        return copy(isActive = false)
    }

    fun hasSameCatalog(itemInfo: ItemInfo): Boolean {
        return this.itemInfo == itemInfo
    }

    fun hasSameQuantity(quantity: Int): Boolean {
        return stock.hasQuantity(quantity)
    }

    fun withId(id: Long): Item = copy(itemId = id)
    fun incrementCatalogVersion(): Item = copy(catalogVersion = catalogVersion + 1)
    fun incrementStockVersion(): Item = copy(stockVersion = stockVersion + 1)

    companion object {
        fun create(
            name: String,
            category: Category,
            price: Int,
            barcode: String?,
            quantity: Int,
        ): Item = Item(
            itemInfo = ItemInfo(name = name, category = category, price = price, barcode = barcode),
            stock = Stock(quantity),
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Item) return false

        if (itemId != other.itemId) return false

        return true
    }

    override fun hashCode(): Int {
        return itemId.hashCode()
    }
}
