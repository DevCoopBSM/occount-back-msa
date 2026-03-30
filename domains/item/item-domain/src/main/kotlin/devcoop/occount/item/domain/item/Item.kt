package devcoop.occount.item.domain.item

data class Item(
    private val itemId: Long = 0L,
    private val itemInfo: ItemInfo,
    private val stock: Stock = Stock(),
    private val isActive: Boolean = true,
) {
    fun getItemId() = itemId
    fun getName() = itemInfo.name()
    fun getCategory() = itemInfo.category()
    fun getPrice() = itemInfo.price()
    fun getBarcode() = itemInfo.barcode()
    fun getQuantity() = stock.getQuantity()
    fun isActive() = isActive

    fun decreaseQuantity(orderQuantity: Int): Item {
        return copy(stock = stock.decreaseQuantity(orderQuantity))
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
