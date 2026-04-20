package devcoop.occount.payment.application.usecase.wallet.refund

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.ChargeReason
import devcoop.occount.payment.domain.wallet.PointTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RefundWalletPointsUseCase(
    private val walletRepository: WalletRepository,
    private val chargeLogRepository: ChargeLogRepository,
) {
    @Transactional
    fun refund(userId: Long, amount: Int, paymentId: Long, detailReason: String): ChargeLog {
        chargeLogRepository.findByPaymentId(paymentId)?.let { return it }

        val wallet = walletRepository.findByUserId(userId)
            ?: throw WalletNotFoundException()

        val beforePoint = wallet.point
        val updatedWallet = walletRepository.save(wallet.charge(amount))

        return chargeLogRepository.save(
            ChargeLog(
                userId = userId,
                paymentId = paymentId,
                pointTransaction = PointTransaction(
                    beforePoint = beforePoint,
                    changeAmount = amount,
                    afterPoint = updatedWallet.point,
                ),
                chargeReason = ChargeReason.REFUND,
                detailReason = detailReason,
            ),
        )
    }
}
