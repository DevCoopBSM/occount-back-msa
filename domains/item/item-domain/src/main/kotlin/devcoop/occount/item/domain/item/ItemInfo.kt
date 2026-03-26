package devcoop.occount.item.domain.item

class ItemInfo(
    private var name: String,
    private var category: Category,
    private var price: Int,
    private var barcode: String? = null,
) {
    fun name() = name
    fun category() = category
    fun price() = price
    fun barcode() = barcode
}
