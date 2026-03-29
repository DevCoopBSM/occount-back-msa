package devcoop.occount.payment.infrastructure.wallet

import devcoop.occount.payment.application.output.WalletPort
import devcoop.occount.payment.application.query.wallet.GetWalletBalanceQueryService
import devcoop.occount.payment.application.usecase.wallet.DeductWalletUseCase
import org.springframework.stereotype.Component

@Component
class DirectWalletAdapter(
    private val deductWalletUseCase: DeductWalletUseCase,
    private val getWalletBalanceQueryService: GetWalletBalanceQueryService,
) : WalletPort {
    override fun getBalance(userId: Long): Int =
        getWalletBalanceQueryService.getBalance(userId).balance

    override fun deduct(userId: Long, amount: Int): Int =
        deductWalletUseCase.deduct(userId, amount).balance
}
