package devcoop.occount.payment.application.usecase.wallet.charge

data class BulkChargeWalletRequest(
    val userIds: List<Long>,
    val amount: Int,
    val reason: String,
)
