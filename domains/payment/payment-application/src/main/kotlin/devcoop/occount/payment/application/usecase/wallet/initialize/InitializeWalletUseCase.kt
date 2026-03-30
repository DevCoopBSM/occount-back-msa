package devcoop.occount.payment.application.usecase.wallet.initialize

import devcoop.occount.payment.application.exception.WalletAlreadyInitializedException
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.domain.wallet.Wallet
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service

@Service
class InitializeWalletUseCase(
    private val walletRepository: WalletRepository,
) {
    fun initialize(userId: Long) {
        try {
            walletRepository.save(Wallet(userId))
        } catch (_: DuplicateKeyException) {
            throw WalletAlreadyInitializedException()
        }
    }
}
