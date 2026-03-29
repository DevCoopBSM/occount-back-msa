package devcoop.occount.payment.application.query.wallet

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.output.WalletRepository
import org.springframework.stereotype.Service

@Service
class GetWalletBalanceQueryService(
    private val pointWalletRepository: WalletRepository,
) {
    fun getBalance(userId: Long): WalletBalanceResponse {
        val pointWallet = pointWalletRepository.findByUserId(userId)
            ?: throw WalletNotFoundException()
        return WalletBalanceResponse(balance = pointWallet.balance)
    }
}
