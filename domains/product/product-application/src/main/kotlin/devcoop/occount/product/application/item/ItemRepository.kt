package devcoop.occount.product.application.item

import devcoop.occount.product.domain.item.Item

interface ItemRepository {
    fun findAll(): List<Item>

    fun findAllWithoutBarcode(): List<Item>

    fun findAllByNameIn(names: List<String>): List<Item>

    fun findAllByItemIds(itemIds: List<Long>): List<Item>

    fun findAllIds(): List<Long>

    fun existsItemByNameIsNotIn(names: List<String>): Boolean

    fun findById(id: Long): Item?

    fun findByBarcode(barcode: String): Item?

    fun save(item: Item): Item

    fun saveAll(items: List<Item>): List<Item>
}
