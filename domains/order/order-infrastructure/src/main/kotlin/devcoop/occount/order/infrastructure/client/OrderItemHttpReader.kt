package devcoop.occount.order.infrastructure.client

import devcoop.occount.order.application.output.OrderItemData
import devcoop.occount.order.application.output.OrderItemReader
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class OrderItemHttpReader(
    private val itemRestClient: RestClient,
) : OrderItemReader {
    override fun findByIds(itemIds: Set<Long>): List<OrderItemData> {
        if (itemIds.isEmpty()) return emptyList()

        val response = itemRestClient.get()
            .uri { builder ->
                builder.path("/items/by-ids")
                itemIds.forEach { id -> builder.queryParam("ids", id) }
                builder.build()
            }
            .retrieve()
            .body(ItemLookupListResponse::class.java)

        return response?.items?.map {
            OrderItemData(
                itemId = it.itemId,
                itemName = it.name,
                itemPrice = it.price,
                isActive = it.isActive,
            )
        } ?: emptyList()
    }

    private data class ItemLookupListResponse(val items: List<ItemLookupResponse>)

    private data class ItemLookupResponse(
        val itemId: Long,
        val name: String,
        val price: Int,
        val isActive: Boolean,
    )
}
