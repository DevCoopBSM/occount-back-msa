package devcoop.occount.item.infrastructure.toss

import devcoop.occount.item.application.item.ItemListResponse
import devcoop.occount.item.application.item.SoldItemListResponse
import devcoop.occount.item.application.item.TossPort
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
