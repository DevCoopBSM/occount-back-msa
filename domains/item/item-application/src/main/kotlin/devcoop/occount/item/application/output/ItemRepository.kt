package devcoop.occount.item.application.output

import devcoop.occount.item.domain.item.Item

interface ItemRepository {
    fun findAll(): List<Item>

    fun findAllWithoutBarcode(): List<Item>

    fun findAllByNameIn(names: List<String>): List<Item>

    fun findAllIds(): List<Long>

    fun existsItemByNameIsNotIn(names: List<String>): Boolean

    fun findById(id: Long): Item?

    fun findByBarcode(barcode: String): Item?

    fun save(item: Item): Item

    fun saveAll(items: List<Item>): List<Item>
}
