package devcoop.occount.item.infrastructure.client.toss

import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange(url = "\${toss.api.url}")
interface TossClient {
    @GetExchange("/items")
    fun getItemList(): TossItemListResponse

    @GetExchange("/sales")
    fun getSoldItemList(): TossSoldItemListResponse
}
