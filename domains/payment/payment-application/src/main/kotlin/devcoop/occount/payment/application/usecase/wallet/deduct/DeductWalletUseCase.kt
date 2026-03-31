package devcoop.occount.payment.application.usecase.wallet.deduct

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.domain.wallet.Wallet
import devcoop.occount.payment.domain.wallet.PointTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeductWalletUseCase(
    private val walletRepository: WalletRepository,
) {
    @Transactional
    fun deduct(userId: Long, deductAmount: Int): PointTransaction {
        val wallet = walletRepository.findByUserId(userId)
            ?: throw WalletNotFoundException()

        val savedWallet = walletRepository.save(wallet.deduct(deductAmount))
        return PointTransaction(
            beforePoint = wallet.point,
            changeAmount = -deductAmount,
            afterPoint = savedWallet.point,
        )
    }

    @Transactional
    fun deduct(deductAmount: Int, wallet: Wallet): PointTransaction {
        val savedWallet = walletRepository.save(wallet.deduct(deductAmount))
        return PointTransaction(
            beforePoint = wallet.point,
            changeAmount = -deductAmount,
            afterPoint = savedWallet.point,
        )
    }
}
