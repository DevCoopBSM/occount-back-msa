package devcoop.occount.order.domain.order

import devcoop.occount.item.domain.item.Item
import devcoop.occount.item.domain.item.ItemNotFoundException

class Order(
    private var items: Map<Long, Item>
) {
    fun decreaseItems(itemId: Long, orderQuantity: Int) {
        val item = items[itemId]
            ?: throw ItemNotFoundException()

        items = items + (itemId to item.decreaseQuantity(orderQuantity))
    }

    fun getItems(): List<Item> = items.values.toList()
}
