package devcoop.occount.payment.application.query.wallet

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.output.WalletRepository
import org.springframework.stereotype.Service

@Service
class GetWalletBalanceQueryService(
    private val walletRepository: WalletRepository,
) {
    fun getBalance(userId: Long): Int {
        val wallet = walletRepository.findByUserId(userId)
            ?: throw WalletNotFoundException()
        return wallet.point
    }
}
