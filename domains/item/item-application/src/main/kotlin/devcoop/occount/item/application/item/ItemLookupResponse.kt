package devcoop.occount.item.application.item

data class ItemLookupResponse(
    val itemId: Long,
    val name: String,
    val barcode: String?,
    val price: Int,
)
