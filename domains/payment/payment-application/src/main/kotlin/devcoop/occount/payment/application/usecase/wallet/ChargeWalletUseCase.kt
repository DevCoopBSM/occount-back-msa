package devcoop.occount.payment.application.usecase.wallet

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.application.usecase.charge.CardChargeUseCase
import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.ChargeLogRepository
import devcoop.occount.payment.domain.vo.PointTransaction
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChargeWalletUseCase(
    private val pointWalletRepository: WalletRepository,
    private val chargeLogRepository: ChargeLogRepository,
    private val cardChargeUseCase: CardChargeUseCase,
) {
    @Transactional
    fun charge(request: ChargeWalletRequest) {
        val pointWallet = pointWalletRepository.findByUserId(request.userId) ?: throw WalletNotFoundException()

        val paymentId = cardChargeUseCase.execute(
            userId = request.userId,
            amount = request.amount,
        ).paymentLogId!!

        val beforeBalance = pointWallet.balance
        val updatedWallet = pointWalletRepository.save(pointWallet.charge(request.amount))
        val afterBalance = updatedWallet.balance

        chargeLogRepository.save(
            ChargeLog(
                userId = request.userId,
                chargeAmount = request.amount,
                paymentId = paymentId,
                pointTransaction = PointTransaction(
                    beforePoint = beforeBalance,
                    afterPoint = afterBalance,
                ),
            )
        )
    }
}
