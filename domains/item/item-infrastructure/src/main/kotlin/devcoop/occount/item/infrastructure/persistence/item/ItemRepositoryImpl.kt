package devcoop.occount.item.infrastructure.persistence.item

import devcoop.occount.item.application.output.ItemRepository
import devcoop.occount.item.domain.item.Item
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

    override fun findAllIds(): List<Long> {
        return itemPersistenceRepository.findAllItemIds()
    }

    override fun existsItemByNameIsNotIn(names: List<String>): Boolean {
        return itemPersistenceRepository.existsByItemInfoNameNotIn(names)
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
        return itemPersistenceRepository.save(ItemPersistenceMapper.toEntity(item))
            .let(ItemPersistenceMapper::toDomain)
    }

    override fun saveAll(items: List<Item>): List<Item> {
        return itemPersistenceRepository.saveAll(items.map(ItemPersistenceMapper::toEntity))
            .map(ItemPersistenceMapper::toDomain)
    }
}
