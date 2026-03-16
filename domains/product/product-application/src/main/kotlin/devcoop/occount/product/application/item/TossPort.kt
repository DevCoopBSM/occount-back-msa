package devcoop.occount.product.application.item

interface TossPort {
    fun getItemList(): ItemListResponse

    fun getSoldItems(): SoldItemListResponse
}
