package devcoop.occount.item.domain.item

data class Stock(
    private val quantity: Int = 0,
) {
    fun getQuantity() = quantity
    fun hasQuantity(quantity: Int) = this.quantity == quantity

    fun decreaseQuantity(orderQuantity: Int): Stock {
        validateQuantity(orderQuantity)
        return copy(quantity = quantity - orderQuantity)
    }

    private fun validateQuantity(orderQuantity: Int) {
        if (quantity < orderQuantity) {
            throw ItemStockNegativeException()
        }
    }
}
