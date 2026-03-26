package devcoop.occount.item.domain.item

class Stock(
    private var quantity: Int = 0,
) {
    fun getQuantity() = quantity

    fun decreaseQuantity(orderQuantity: Int) {
        validateQuantity(orderQuantity)
        this.quantity -= orderQuantity
    }

    private fun validateQuantity(orderQuantity: Int) {
        if (quantity < orderQuantity) {
            throw ItemStockNegativeException()
        }
    }
}
