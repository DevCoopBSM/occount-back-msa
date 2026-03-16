package devcoop.occount.product.application.item

data class ItemLookupResponse(
    val itemId: Long,
    val name: String,
    val barcode: String?,
    val price: Int,
)
