package devcoop.occount.item.api.support

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.domain.item.Category
import devcoop.occount.item.domain.item.Item
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
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
): Item {
    val item = Item.create(name = name, category = category, price = price, barcode = barcode, quantity = quantity)
        .withId(itemId)
    return if (isActive) item else item.deactivate()
}

class FakeItemRepository(
    initialItems: List<Item> = emptyList(),
) : ItemRepository {
    private val itemsById = linkedMapOf<Long, Item>().apply {
        initialItems.forEach { item -> put(item.getItemId(), item) }
    }

    var saveOptimisticLockFailuresRemaining: Int = 0
    var saveCatalogOptimisticLockFailuresRemaining: Int = 0
    var saveStockOptimisticLockFailuresRemaining: Int = 0

    override fun findAll(): List<Item> = itemsById.values.filter(Item::isActive)

    override fun findAllWithoutBarcode(): List<Item> {
        return itemsById.values.filter { it.isActive() && it.getBarcode() == null }
    }

    override fun findAllByNameIn(names: List<String>): List<Item> {
        return itemsById.values.filter { it.getName() in names }
    }

    override fun findAllByItemIds(itemIds: List<Long>): List<Item> {
        return itemIds.mapNotNull(itemsById::get)
    }

    override fun searchByName(query: String): List<Item> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return itemsById.values.filter {
            it.isActive() && it.getName().contains(trimmed, ignoreCase = true)
        }
    }

    override fun findById(id: Long): Item? = itemsById[id]

    override fun findByBarcode(barcode: String): Item? {
        return itemsById.values.firstOrNull { it.getBarcode() == barcode }
    }

    override fun save(item: Item): Item {
        if (saveOptimisticLockFailuresRemaining > 0) {
            saveOptimisticLockFailuresRemaining -= 1
            throw OptimisticLockingFailureException("simulated optimistic lock failure")
        }

        val persistedItem = persistBoth(item)
        itemsById[persistedItem.getItemId()] = persistedItem
        return persistedItem
    }

    override fun saveCatalog(item: Item): Item {
        if (saveCatalogOptimisticLockFailuresRemaining > 0) {
            saveCatalogOptimisticLockFailuresRemaining -= 1
            throw OptimisticLockingFailureException("simulated optimistic lock failure")
        }

        val persistedItem = item.incrementCatalogVersion()
        itemsById[persistedItem.getItemId()] = persistedItem
        return persistedItem
    }

    override fun saveCatalogs(items: List<Item>): List<Item> = items.map(::saveCatalog)

    override fun saveStock(item: Item): Item {
        if (saveStockOptimisticLockFailuresRemaining > 0) {
            saveStockOptimisticLockFailuresRemaining -= 1
            throw OptimisticLockingFailureException("simulated optimistic lock failure")
        }

        val persistedItem = item.incrementStockVersion()
        itemsById[persistedItem.getItemId()] = persistedItem
        return persistedItem
    }

    override fun saveStocks(items: List<Item>): List<Item> = items.map(::saveStock)

    private fun persistBoth(item: Item): Item {
        val withId = if (item.getItemId() == 0L) item.withId(nextId()) else item
        return withId.incrementCatalogVersion().incrementStockVersion()
    }

    private fun nextId(): Long = (itemsById.keys.maxOrNull() ?: 0L) + 1L
}

class TestTransactionManager : PlatformTransactionManager {
    override fun getTransaction(definition: TransactionDefinition?): TransactionStatus {
        return SimpleTransactionStatus()
    }

    override fun commit(status: TransactionStatus) = Unit

    override fun rollback(status: TransactionStatus) = Unit
}

fun mockMvc(vararg controllers: Any): MockMvc {
    val messageConverter = MappingJackson2HttpMessageConverter(
        jacksonObjectMapper(),
    )
    return MockMvcBuilders.standaloneSetup(*controllers)
        .setControllerAdvice(ApiAdviceHandler())
        .setMessageConverters(messageConverter)
        .build()
}
