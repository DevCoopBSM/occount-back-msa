package devcoop.occount.item.infrastructure.persistence.item

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.domain.item.Item
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Repository

@Repository
class ItemRepositoryImpl(
    private val itemPersistenceRepository: ItemPersistenceRepository,
) : ItemRepository {
    override fun findAll(): List<Item> {
        return itemPersistenceRepository.findAllByIsActiveTrue()
            .map(ItemPersistenceMapper::toDomain)
    }

    override fun findAllWithoutBarcode(): List<Item> {
        return itemPersistenceRepository.findAllByItemInfoBarcodeIsNullAndIsActiveTrue()
            .map(ItemPersistenceMapper::toDomain)
    }

    override fun findAllByNameIn(names: List<String>): List<Item> {
        return itemPersistenceRepository.findAllByItemInfoNameIn(names)
            .map(ItemPersistenceMapper::toDomain)
    }

    override fun findAllByItemIds(itemIds: List<Long>): List<Item> {
        return itemPersistenceRepository.findAllByItemIdIn(itemIds)
            .map(ItemPersistenceMapper::toDomain)
    }

    override fun searchByName(query: String): List<Item> {
        val sanitized = query.trim().replace(BOOLEAN_MODE_METACHARS, " ")
        if (sanitized.isBlank()) return emptyList()

        return itemPersistenceRepository.searchByNameFulltext("\"$sanitized\"")
            .map(ItemPersistenceMapper::toDomain)
    }

    override fun findById(id: Long): Item? {
        return itemPersistenceRepository.findById(id)
            .map(ItemPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun findByBarcode(barcode: String): Item? {
        return itemPersistenceRepository.findByItemInfoBarcode(barcode)
            ?.let(ItemPersistenceMapper::toDomain)
    }

    override fun save(item: Item): Item {
        if (isNewItem(item)) {
            return persistNew(item)
        }

        updateCatalog(item)
        updateStock(item)

        return findPersistedItem(item.getItemId())
    }

    override fun saveCatalog(item: Item): Item {
        if (isNewItem(item)) {
            return persistNew(item)
        }

        updateCatalog(item)

        return findPersistedItem(item.getItemId())
    }

    override fun saveCatalogs(items: List<Item>): List<Item> {
        return items.map(::saveCatalog)
    }

    override fun saveStock(item: Item): Item {
        if (isNewItem(item)) {
            return persistNew(item)
        }

        updateStock(item)

        return findPersistedItem(item.getItemId())
    }

    override fun saveStocks(items: List<Item>): List<Item> {
        return items.map(::saveStock)
    }

    private fun persistNew(item: Item): Item {
        return itemPersistenceRepository.save(ItemPersistenceMapper.toEntity(item))
            .let(ItemPersistenceMapper::toDomain)
    }

    private fun isNewItem(item: Item): Boolean {
        return item.getItemId() == 0L || !itemPersistenceRepository.existsById(item.getItemId())
    }

    private fun updateCatalog(item: Item) {
        val updatedRowCount = itemPersistenceRepository.updateCatalog(
            itemId = item.getItemId(),
            name = item.getName(),
            category = item.getCategory(),
            price = item.getPrice(),
            barcode = item.getBarcode(),
            isActive = item.isActive(),
            catalogVersion = item.getCatalogVersion(),
        )

        if (updatedRowCount == 0) {
            throw OptimisticLockingFailureException("catalog optimistic lock failed for itemId=${item.getItemId()}")
        }
    }

    private fun updateStock(item: Item) {
        val updatedRowCount = itemPersistenceRepository.updateStock(
            itemId = item.getItemId(),
            quantity = item.getQuantity(),
            stockVersion = item.getStockVersion(),
        )

        if (updatedRowCount == 0) {
            throw OptimisticLockingFailureException("stock optimistic lock failed for itemId=${item.getItemId()}")
        }
    }

    private fun findPersistedItem(itemId: Long): Item {
        return itemPersistenceRepository.findById(itemId)
            .map(ItemPersistenceMapper::toDomain)
            .orElseThrow {
                IllegalStateException("Persisted item not found. itemId=$itemId")
            }
    }

    companion object {
        private val BOOLEAN_MODE_METACHARS = Regex("""[+\-><()~*"@]""")
    }
}
