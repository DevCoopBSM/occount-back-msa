package devcoop.occount.order.application.shared

import devcoop.occount.order.application.exception.OrderInvalidTotalPriceException

data class OrderItemRequest(
    val itemId: Long,
    val itemName: String,
    val itemPrice: Int,
    val quantity: Int,
) {
    fun validate() {
        if (quantity <= 0) {
            throw OrderInvalidTotalPriceException()
        }

        if (itemPrice < 0) {
            throw OrderInvalidTotalPriceException()
        }
    }
}
