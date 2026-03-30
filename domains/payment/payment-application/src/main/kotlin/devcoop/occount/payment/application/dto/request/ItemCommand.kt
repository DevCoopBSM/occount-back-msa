package devcoop.occount.payment.application.dto.request

import devcoop.occount.payment.application.shared.PaymentItem

data class ItemCommand(
    val name: String,
    val price: Int,
    val quantity: Int,
    val total: Int
) {
    companion object {
        fun from(item: PaymentItem): ItemCommand {
            return ItemCommand(
                name = item.itemName,
                price = item.itemPrice,
                quantity = item.quantity,
                total = item.totalPrice,
            )
        }

        fun charge(amount: Int): ItemCommand {
            return ItemCommand(
                name = "포인트 충전",
                price = amount,
                quantity = 1,
                total = amount,
            )
        }
    }
}
