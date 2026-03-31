package devcoop.occount.item.infrastructure.client.toss

import devcoop.occount.item.application.output.SoldItemPayload
import devcoop.occount.item.application.output.TossItemPayload
import devcoop.occount.item.application.output.TossItemPort
import org.springframework.stereotype.Component

@Component
class TossService(
    private val tossClient: TossClient,
) : TossItemPort {
    override fun getItems(): List<TossItemPayload> {
        return tossClient.getItemList().items
            .map { item ->
                TossItemPayload(
                    itemId = item.itemId,
                    name = item.name,
                    category = item.category,
                    price = item.price,
                    barcode = item.barcode,
                )
            }
    }

    override fun getSoldItems(): List<SoldItemPayload> {
        return tossClient.getSoldItemList().soldItems
            .map { soldItem ->
                SoldItemPayload(
                    name = soldItem.name,
                    quantity = soldItem.quantity,
                )
            }
    }
}
