package devcoop.occount.payment.application.usecase.wallet

data class ChargeWalletRequest(
    val userId: Long,
    val amount: Int,
)
