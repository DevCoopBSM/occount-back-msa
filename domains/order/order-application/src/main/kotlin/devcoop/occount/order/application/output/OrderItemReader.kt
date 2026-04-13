package devcoop.occount.order.application.output

interface OrderItemReader {
    fun findByIds(itemIds: Set<Long>): List<OrderItemData>
}

data class OrderItemData(
    val itemId: Long,
    val itemName: String,
    val itemPrice: Int,
    val isActive: Boolean,
)
