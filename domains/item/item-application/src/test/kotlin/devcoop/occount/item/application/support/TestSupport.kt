package devcoop.occount.item.application.support

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.application.output.SoldItemPayload
import devcoop.occount.item.application.output.TossItemPayload
import devcoop.occount.item.application.output.TossItemPort
import devcoop.occount.item.domain.item.Category
import devcoop.occount.item.domain.item.Item
import devcoop.occount.item.domain.item.ItemInfo
import devcoop.occount.item.domain.item.Stock
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus

fun itemFixture(
    itemId: Long,
    name: String = "Snack",
    category: Category = Category.과자,
    price: Int = 1500,
    barcode: String? = null,
    quantity: Int = 0,
    isActive: Boolean = true,
    version: Long = 0L,
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
        version = version,
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
    var saveAllOptimisticLockFailuresRemaining: Int = 0

    override fun findAll(): List<Item> {
        return itemsById.values.filter(Item::isActive)
    }

    override fun findAllWithoutBarcode(): List<Item> {
        return itemsById.values.filter { it.isActive() && it.getBarcode() == null }
    }

    override fun findAllByNameIn(names: List<String>): List<Item> {
        return itemsById.values.filter { it.getName() in names }
    }

    override fun findAllByItemIds(itemIds: List<Long>): List<Item> {
        return itemIds.mapNotNull(itemsById::get)
    }

    override fun findById(id: Long): Item? {
        return itemsById[id]
    }

    override fun findByBarcode(barcode: String): Item? {
        return itemsById.values.firstOrNull { it.getBarcode() == barcode }
    }

    override fun save(item: Item): Item {
        val persistedItem = persist(item)
        itemsById[persistedItem.getItemId()] = persistedItem
        saveCount += 1
        lastSavedItem = persistedItem
        return persistedItem
    }

    override fun saveAll(items: List<Item>): List<Item> {
        saveAllCount += 1
        if (saveAllOptimisticLockFailuresRemaining > 0) {
            saveAllOptimisticLockFailuresRemaining -= 1
            throw OptimisticLockingFailureException("simulated optimistic lock failure")
        }

        val persistedItems = items.map(::persist)
        persistedItems.forEach { item -> itemsById[item.getItemId()] = item }
        lastSavedItems = persistedItems
        return persistedItems
    }

    fun allItems(): List<Item> {
        return itemsById.values.toList()
    }

    private fun persist(item: Item): Item {
        return Item(
            itemId = item.getItemId(),
            itemInfo = ItemInfo(
                name = item.getName(),
                category = item.getCategory(),
                price = item.getPrice(),
                barcode = item.getBarcode(),
            ),
            stock = Stock(item.getQuantity()),
            isActive = item.isActive(),
            version = item.getVersion() + 1,
        )
    }
}

class FakeTossItemPort(
    var itemPayloads: List<TossItemPayload> = emptyList(),
    var soldItemPayloads: List<SoldItemPayload> = emptyList(),
) : TossItemPort {
    var getItemsCount: Int = 0
        private set
    var getSoldItemsCount: Int = 0
        private set

    override fun getItems(): List<TossItemPayload> {
        getItemsCount += 1
        return itemPayloads
    }

    override fun getSoldItems(): List<SoldItemPayload> {
        getSoldItemsCount += 1
        return soldItemPayloads
    }
}

class TestTransactionManager : PlatformTransactionManager {
    override fun getTransaction(definition: TransactionDefinition?): TransactionStatus {
        return SimpleTransactionStatus()
    }

    override fun commit(status: TransactionStatus) = Unit

    override fun rollback(status: TransactionStatus) = Unit
}
