package devcoop.occount.item.infrastructure.persistence.item

import devcoop.occount.item.domain.item.Item
import devcoop.occount.item.domain.item.ItemInfo
import devcoop.occount.item.domain.item.Stock

object ItemPersistenceMapper {
    fun toDomain(entity: ItemJpaEntity): Item {
        val itemInfoEntity = entity.getItemInfo()
        return Item(
            itemId = entity.getItemId(),
            itemInfo = ItemInfo(
                name = itemInfoEntity.getName(),
                category = itemInfoEntity.getCategory(),
                price = itemInfoEntity.getPrice(),
                barcode = itemInfoEntity.getBarcode(),
            ),
            stock = Stock(quantity = entity.getStock().getQuantity()),
            isActive = entity.isActive(),
        )
    }

    fun toEntity(domain: Item): ItemJpaEntity {
        return ItemJpaEntity(
            itemId = domain.getItemId(),
            itemInfo = ItemInfoJpaEmbeddable(
                name = domain.getName(),
                category = domain.getCategory(),
                price = domain.getPrice(),
                barcode = domain.getBarcode(),
            ),
            stock = StockJpaEmbeddable(
                quantity = domain.getQuantity(),
            ),
            isActive = domain.isActive(),
        )
    }
}
