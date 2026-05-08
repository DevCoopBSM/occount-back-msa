package devcoop.occount.payment.application.shared

import devcoop.occount.payment.application.query.wallet.GetWalletPointQueryService
import devcoop.occount.payment.application.usecase.payment.CardOnlyPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.MixedPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.PayWithPointsUseCase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PaymentFacade(
    private val payWithPointsUseCase: PayWithPointsUseCase,
    private val mixedPaymentUseCase: MixedPaymentUseCase,
    private val cardOnlyPaymentUseCase: CardOnlyPaymentUseCase,
    private val getWalletPointQueryService: GetWalletPointQueryService,
) {
    fun execute(userId: Long?, kioskId: String, details: PaymentDetails, paymentKey: Long? = null): PaymentResponse {
        if (userId == null) {
            return cardOnlyPaymentUseCase.execute(null, kioskId, details, paymentKey)
        }
        val balance = runCatching { getWalletPointQueryService.getPoint(userId) }
            .onFailure { log.warn("지갑 포인트 조회 실패, 카드 결제로 진행 - userId={}", userId, it) }
            .getOrDefault(0)
        return when {
            balance >= details.totalAmount -> payWithPointsUseCase.execute(userId, details)
            balance > 0 -> mixedPaymentUseCase.execute(userId, kioskId, details, paymentKey)
            else -> cardOnlyPaymentUseCase.execute(userId, kioskId, details, paymentKey)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PaymentFacade::class.java)
    }
}
