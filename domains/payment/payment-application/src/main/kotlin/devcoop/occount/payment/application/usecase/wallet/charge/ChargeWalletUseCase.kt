package devcoop.occount.payment.application.usecase.wallet.charge

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.ChargeReason
import devcoop.occount.payment.domain.wallet.PointTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChargeWalletUseCase(
    private val walletRepository: WalletRepository,
    private val chargeLogRepository: ChargeLogRepository,
) {
    @Transactional
    fun charge(request: ChargeWalletRequest) {
        val wallet = walletRepository.findByUserId(request.userId) ?: throw WalletNotFoundException()

        val beforePoint = wallet.point
        val updatedWallet = walletRepository.save(wallet.charge(request.amount))
        val afterPoint = updatedWallet.point

        chargeLogRepository.save(
            ChargeLog(
                userId = request.userId,
                pointTransaction = PointTransaction(
                    beforePoint = beforePoint,
                    changeAmount = request.amount,
                    afterPoint = afterPoint,
                ),
                chargeReason = request.reason,
            )
        )
    }
}
