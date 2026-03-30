package devcoop.occount.item.application.support

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.output.SoldItemPayload
import devcoop.occount.item.application.output.TossItemPayload
import devcoop.occount.item.application.output.TossItemPort
import devcoop.occount.item.domain.item.Category
import devcoop.occount.item.domain.item.Item
import devcoop.occount.item.domain.item.ItemInfo
import devcoop.occount.item.domain.item.Stock

fun itemFixture(
    itemId: Long,
    name: String = "Snack",
    category: Category = Category.과자,
    price: Int = 1500,
    barcode: String? = null,
    quantity: Int = 0,
    isActive: Boolean = true,
): Item {
    return Item(
        itemId = itemId,
        itemInfo = ItemInfo(
            name = name,
            category = category,
            price = price,
            barcode = barcode,
        ),
        stock = Stock(quantity),
        isActive = isActive,
    )
}

class FakeItemRepository(
    initialItems: List<Item> = emptyList(),
) : ItemRepository {
    private val itemsById = linkedMapOf<Long, Item>().apply {
        initialItems.forEach { item -> put(item.getItemId(), item) }
    }

    var saveCount: Int = 0
        private set
    var saveAllCount: Int = 0
        private set
    var lastSavedItem: Item? = null
        private set
    var lastSavedItems: List<Item> = emptyList()
        private set

    override fun findAll(): List<Item> {
        return itemsById.values.filter(Item::isActive)
    }

    override fun findAllWithoutBarcode(): List<Item> {
        return itemsById.values.filter { it.isActive() && it.getBarcode() == null }
    }

    override fun findAllByNameIn(names: List<String>): List<Item> {
        return itemsById.values.filter { it.getName() in names }
    }

    override fun findAllIds(): List<Long> {
        return itemsById.keys.toList()
    }

    override fun existsItemByNameIsNotIn(names: List<String>): Boolean {
        return itemsById.values.any { it.getName() !in names }
    }

    override fun findById(id: Long): Item? {
        return itemsById[id]
    }

    override fun findByBarcode(barcode: String): Item? {
        return itemsById.values.firstOrNull { it.getBarcode() == barcode }
    }

    override fun save(item: Item): Item {
        itemsById[item.getItemId()] = item
        saveCount += 1
        lastSavedItem = item
        return item
    }

    override fun saveAll(items: List<Item>): List<Item> {
        items.forEach { item -> itemsById[item.getItemId()] = item }
        saveAllCount += 1
        lastSavedItems = items
        return items
    }

    fun allItems(): List<Item> {
        return itemsById.values.toList()
    }
}

class FakeTossItemPort(
    var itemPayloads: List<TossItemPayload> = emptyList(),
    var soldItemPayloads: List<SoldItemPayload> = emptyList(),
) : TossItemPort {
    override fun getItems(): List<TossItemPayload> {
        return itemPayloads
    }

    override fun getSoldItems(): List<SoldItemPayload> {
        return soldItemPayloads
    }
}
