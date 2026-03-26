package devcoop.occount.order.domain.order

import devcoop.occount.item.domain.item.Item
import devcoop.occount.item.domain.item.ItemNotFoundException

class Order(
    private var items: Map<Long, Item>
) {
    fun decreaseItems(itemId: Long, orderQuantity: Int) {
        items[itemId]
            ?.decreaseQuantity(orderQuantity)
            ?: throw ItemNotFoundException()
    }

    fun getItems(): List<Item> = items.values.toList()
}
