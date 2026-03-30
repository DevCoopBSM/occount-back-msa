package devcoop.occount.payment.application.usecase.wallet

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.application.query.wallet.WalletBalanceResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeductWalletUseCase(
    private val pointWalletRepository: WalletRepository,
) {
    @Transactional
    fun deduct(userId: Long, amount: Int): WalletBalanceResponse {
        val pointWallet = pointWalletRepository.findByUserId(userId)
            ?: throw WalletNotFoundException()

        val savedWallet = pointWalletRepository.save(pointWallet.deduct(amount))
        return WalletBalanceResponse(balance = savedWallet.balance)
    }
}
