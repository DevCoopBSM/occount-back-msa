package devcoop.occount.payment.application.usecase.wallet.admincharge

import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.ChargeReason
import devcoop.occount.payment.domain.wallet.PointTransaction
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 어드민 충전의 사용자 단건 처리 단위.
 * 사용자별 독립 트랜잭션으로 분리하여, 한 사용자의 실패가 다른 사용자의 충전 커밋을 롤백시키지 않도록 한다.
 */
@Component
class WalletChargeProcessor(
    private val walletRepository: WalletRepository,
    private val chargeLogRepository: ChargeLogRepository,
) {
    @Transactional
    fun charge(userId: Long, amount: Int, reason: String, adminId: Long) {
        val wallet = walletRepository.findByUserId(userId) ?: throw WalletNotFoundException()

        val beforePoint = wallet.point
        val updatedWallet = walletRepository.save(wallet.charge(amount))
        val afterPoint = updatedWallet.point

        chargeLogRepository.save(
            ChargeLog(
                userId = userId,
                pointTransaction = PointTransaction(
                    beforePoint = beforePoint,
                    changeAmount = amount,
                    afterPoint = afterPoint,
                ),
                chargeReason = ChargeReason.ADMIN,
                detailReason = reason,
                chargedBy = adminId,
            ),
        )
    }
}
