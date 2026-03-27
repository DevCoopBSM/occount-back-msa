package devcoop.occount.item.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ItemPersistenceRepository: JpaRepository<ItemJpaEntity, Long> {
    fun findAllByItemInfoNameIn(names: List<String>): MutableList<ItemJpaEntity>
    fun existsByItemInfoNameNotIn(names: List<String>): Boolean
    fun findAllByIsActiveTrue(): MutableList<ItemJpaEntity>
    fun findAllByItemInfoBarcodeIsNullAndIsActiveTrue(): MutableList<ItemJpaEntity>
    fun findAllByItemIdIn(itemIds: List<Long>): MutableList<ItemJpaEntity>
    fun findByItemInfoBarcode(barcode: String): ItemJpaEntity?

    @Query("select i.itemId from ItemJpaEntity i")
    fun findAllItemIds(): MutableList<Long>
}
