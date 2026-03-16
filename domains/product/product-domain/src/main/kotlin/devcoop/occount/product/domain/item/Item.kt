package devcoop.occount.product.domain.item

class Item(
    private var itemId: Long = 0L,
    private var itemInfo: ItemInfo,
    private var stock: Stock = Stock(),
    private var isActive: Boolean = true,
) {
    fun getItemId() = itemId
    fun getName() = itemInfo.name()
    fun getCategory() = itemInfo.category()
    fun getPrice() = itemInfo.price()
    fun getBarcode() = itemInfo.barcode()
    fun getQuantity() = stock.getQuantity()
    fun isActive() = isActive

    fun decreaseQuantity(orderQuantity: Int) {
        stock.decreaseQuantity(orderQuantity)
    }

    fun deactivate() {
        this.isActive = false
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
