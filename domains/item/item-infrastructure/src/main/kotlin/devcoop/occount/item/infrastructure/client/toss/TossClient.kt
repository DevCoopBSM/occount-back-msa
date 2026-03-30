package devcoop.occount.item.infrastructure.client.toss

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange(url = "\${toss.api.url}")
interface TossClient {
    @GetMapping("/items")
    fun getItemList(): TossItemListResponse

    @GetMapping("/sales")
    fun getSoldItemList(): TossSoldItemListResponse
}
