package devcoop.occount.order.application.order

data class OrderInfo(
    val itemId: Long,
    val itemName: String,
    val itemPrice: Int,
    val orderQuantity: Int,
    val totalPrice: Int,
) {
    fun validate() {
        if (itemPrice * orderQuantity != totalPrice) {
            throw OrderInvalidTotalPriceException()
        }
    }
}
