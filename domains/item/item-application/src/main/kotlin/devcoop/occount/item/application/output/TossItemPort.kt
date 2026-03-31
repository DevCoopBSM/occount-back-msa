package devcoop.occount.item.application.output

interface TossItemPort {
    fun getItems(): List<TossItemPayload>

    fun getSoldItems(): List<SoldItemPayload>
}
