package devcoop.occount.item.infrastructure.persistence.item

import devcoop.occount.item.domain.item.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ItemPersistenceRepository : JpaRepository<ItemJpaEntity, Long> {
    fun findAllByItemInfoNameIn(names: List<String>): MutableList<ItemJpaEntity>
    fun findAllByItemIdIn(itemIds: List<Long>): MutableList<ItemJpaEntity>
    fun findAllByIsActiveTrue(): MutableList<ItemJpaEntity>
    fun findAllByItemInfoBarcodeIsNullAndIsActiveTrue(): MutableList<ItemJpaEntity>
    fun findByItemInfoBarcode(barcode: String): ItemJpaEntity?

    @Query(
        value = """
            SELECT i.* FROM item i
            WHERE i.is_active = TRUE
              AND MATCH(i.name) AGAINST (:query IN BOOLEAN MODE)
            ORDER BY MATCH(i.name) AGAINST (:query IN BOOLEAN MODE) DESC
        """,
        nativeQuery = true,
    )
    fun searchByNameFulltext(@Param("query") query: String): List<ItemJpaEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ItemJpaEntity item
        set item.itemInfo.name = :name,
            item.itemInfo.category = :category,
            item.itemInfo.price = :price,
            item.itemInfo.barcode = :barcode,
            item.isActive = :isActive,
            item.catalogVersion = item.catalogVersion + 1
        where item.itemId = :itemId
          and item.catalogVersion = :catalogVersion
        """
    )
    fun updateCatalog(
        @Param("itemId") itemId: Long,
        @Param("name") name: String,
        @Param("category") category: Category,
        @Param("price") price: Int,
        @Param("barcode") barcode: String?,
        @Param("isActive") isActive: Boolean,
        @Param("catalogVersion") catalogVersion: Long,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update ItemJpaEntity item
        set item.stock.quantity = :quantity,
            item.stockVersion = item.stockVersion + 1
        where item.itemId = :itemId
          and item.stockVersion = :stockVersion
        """
    )
    fun updateStock(
        @Param("itemId") itemId: Long,
        @Param("quantity") quantity: Int,
        @Param("stockVersion") stockVersion: Long,
    ): Int
}
