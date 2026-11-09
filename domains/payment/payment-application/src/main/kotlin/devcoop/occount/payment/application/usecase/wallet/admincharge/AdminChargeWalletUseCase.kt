package devcoop.occount.payment.application.usecase.wallet.admincharge

import devcoop.occount.core.common.exception.BusinessBaseException
import devcoop.occount.payment.application.exception.WalletNotFoundException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service

/**
 * 어드민이 여러 회원에게 포인트를 일괄 충전한다.
 * 사용자별 독립 트랜잭션([WalletChargeProcessor])으로 처리하여 부분 성공을 허용하고, 결과 리포트를 반환한다.
 * 이 클래스 자체에는 트랜잭션을 두지 않는다(성공 건은 각자 커밋되어야 하므로).
 */
@Service
class AdminChargeWalletUseCase(
    private val walletChargeProcessor: WalletChargeProcessor,
) {
    fun charge(command: AdminChargeCommand): AdminChargeResult {
        val results = command.userIds.distinct().map { userId ->
            try {
                walletChargeProcessor.charge(userId, command.amount, command.reason, command.adminId)
                AdminChargeItemResult.charged(userId)
            } catch (e: WalletNotFoundException) {
                AdminChargeItemResult.failed(userId, "WALLET_NOT_FOUND")
            } catch (e: OptimisticLockingFailureException) {
                // 동시 수정 충돌 — 해당 사용자만 실패로 격리하고 나머지 사용자는 계속 충전한다.
                AdminChargeItemResult.failed(userId, "CONCURRENT_MODIFICATION")
            } catch (e: BusinessBaseException) {
                AdminChargeItemResult.failed(userId, e.errorMessage.name)
            }
        }
        return AdminChargeResult(results)
    }
}
