package devcoop.occount.payment.application.usecase.wallet.charge

data class ChargeWalletRequest(
    val userId: Long,
    val amount: Int,
)
