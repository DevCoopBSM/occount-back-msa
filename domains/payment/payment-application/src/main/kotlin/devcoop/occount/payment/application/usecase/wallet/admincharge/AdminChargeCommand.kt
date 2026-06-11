package devcoop.occount.payment.application.usecase.wallet.admincharge

data class AdminChargeCommand(
    val adminId: Long,
    val userIds: List<Long>,
    val amount: Int,
    val reason: String,
)
