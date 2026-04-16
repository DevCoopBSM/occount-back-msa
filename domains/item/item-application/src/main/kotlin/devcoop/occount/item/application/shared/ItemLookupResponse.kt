package devcoop.occount.item.application.shared

data class ItemLookupResponse(
    val itemId: Long,
    val name: String,
    val barcode: String?,
    val price: Int,
    val isActive: Boolean = true,
)
