package devcoop.occount.order.infrastructure.persistence.order

data class SalesRankingItemJpaProjection(
    val itemId: Long,
    val itemName: String,
    val soldQuantity: Long,
)
