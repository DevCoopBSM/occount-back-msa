package devcoop.occount.payment.application.usecase.wallet.charge

import devcoop.occount.payment.domain.wallet.ChargeReason

data class ChargeWalletRequest(
    val userId: Long,
    val amount: Int,
    val reason: ChargeReason = ChargeReason.PURCHASE,
)
