package devcoop.occount.product.infrastructure.toss

import devcoop.occount.product.application.item.ItemListResponse
import devcoop.occount.product.application.item.SoldItemListResponse
import devcoop.occount.product.application.item.TossPort
import org.springframework.stereotype.Component

@Component
class TossService(
    private val tossClient: TossClient,
) : TossPort {
    override fun getItemList(): ItemListResponse {
        return tossClient.getItemList()
    }

    override fun getSoldItems(): SoldItemListResponse {
        return tossClient.getSoldItemList()
    }
}
