package devcoop.occount.item.infrastructure.persistence.item

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ItemPersistenceRepository : JpaRepository<ItemJpaEntity, Long> {
    fun findAllByItemInfoNameIn(names: List<String>): MutableList<ItemJpaEntity>
    fun findAllByItemIdIn(itemIds: List<Long>): MutableList<ItemJpaEntity>
    fun findAllByIsActiveTrue(): MutableList<ItemJpaEntity>
    fun findAllByItemInfoBarcodeIsNullAndIsActiveTrue(): MutableList<ItemJpaEntity>
    fun findByItemInfoBarcode(barcode: String): ItemJpaEntity?
}
