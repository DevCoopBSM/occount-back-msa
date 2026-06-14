package devcoop.occount.payment.application.usecase.wallet.charge

import devcoop.occount.payment.application.exception.BulkChargeWalletNotFoundException
import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.ChargeReason
import devcoop.occount.payment.domain.wallet.PointTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BulkChargeWalletUseCase(
    private val walletRepository: WalletRepository,
    private val chargeLogRepository: ChargeLogRepository,
) {
    @Transactional
    fun charge(request: BulkChargeWalletRequest) {
        val wallets = request.userIds
            .distinct()
            .map { userId ->
                walletRepository.findByUserId(userId) ?: throw BulkChargeWalletNotFoundException()
            }

        val chargedWallets = wallets.map { wallet ->
            wallet to wallet.charge(request.amount)
        }

        chargedWallets.forEach { (_, chargedWallet) ->
            walletRepository.save(chargedWallet)
        }

        chargeLogRepository.saveAll(
            chargedWallets.map { (wallet, chargedWallet) ->
                ChargeLog(
                    userId = wallet.userId,
                    pointTransaction = PointTransaction(
                        beforePoint = wallet.point,
                        changeAmount = request.amount,
                        afterPoint = chargedWallet.point,
                    ),
                    chargeReason = ChargeReason.REWARD,
                    detailReason = request.reason,
                )
            },
        )
    }
}
