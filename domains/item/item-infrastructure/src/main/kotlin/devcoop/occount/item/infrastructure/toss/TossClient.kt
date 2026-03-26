package devcoop.occount.item.infrastructure.toss

import devcoop.occount.item.application.item.ItemListResponse
import devcoop.occount.item.application.item.SoldItemListResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange(url = "\${toss.api.url}")
interface TossClient {
    @GetMapping("/items")
    fun getItemList(): ItemListResponse

    @GetMapping("/sales")
    fun getSoldItemList(): SoldItemListResponse
}
