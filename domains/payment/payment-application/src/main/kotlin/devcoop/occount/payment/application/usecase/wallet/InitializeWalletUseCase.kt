package devcoop.occount.payment.application.usecase.wallet

import devcoop.occount.payment.application.exception.WalletAlreadyInitializedException
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.domain.Wallet
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class InitializeWalletUseCase(
    private val pointWalletRepository: WalletRepository,
) {
    fun initialize(userId: Long) {
        try {
            pointWalletRepository.save(Wallet(userId))
        } catch (_: DuplicateKeyException) {
            throw WalletAlreadyInitializedException()
        }
    }
}
