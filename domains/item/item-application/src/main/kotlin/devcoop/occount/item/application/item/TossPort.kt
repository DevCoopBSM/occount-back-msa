package devcoop.occount.item.application.item

interface TossPort {
    fun getItemList(): ItemListResponse

    fun getSoldItems(): SoldItemListResponse
}
