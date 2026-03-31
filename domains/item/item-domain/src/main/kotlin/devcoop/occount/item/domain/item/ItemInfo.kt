package devcoop.occount.item.domain.item

data class ItemInfo(
    private val name: String,
    private val category: Category,
    private val price: Int,
    private val barcode: String? = null,
) {
    fun name() = name
    fun category() = category
    fun price() = price
    fun barcode() = barcode
}
