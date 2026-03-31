package devcoop.occount.item.application.output

import devcoop.occount.item.domain.item.Item

interface ItemRepository {
    fun findAll(): List<Item>

    fun findAllWithoutBarcode(): List<Item>

    fun findAllByNameIn(names: List<String>): List<Item>

    fun findAllByItemIds(itemIds: List<Long>): List<Item>

    fun findById(id: Long): Item?

    fun findByBarcode(barcode: String): Item?

    fun save(item: Item): Item

    fun saveCatalog(item: Item): Item

    fun saveCatalogs(items: List<Item>): List<Item>

    fun saveStock(item: Item): Item

    fun saveStocks(items: List<Item>): List<Item>
}
