package devcoop.occount.order.application.output

interface OrderItemReader {
    fun findByIds(itemIds: Set<Long>): List<OrderItemData>
}
