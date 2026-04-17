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
    private val orderPaymentExecutionRepository: OrderPaymentExecutionRepository,
) {
    fun execute(userId: Long?, details: PaymentDetails, paymentKey: String? = null): PaymentResponse {
        if (paymentKey != null && orderPaymentExecutionRepository.isCancellationRequested(paymentKey)) {
            throw PaymentCancelledException()
        }
        if (userId == null) {
            return cardOnlyPaymentUseCase.execute(null, details, paymentKey)
        }
        val balance = runCatching { getWalletPointQueryService.getPoint(userId) }
            .onFailure { log.warn("지갑 포인트 조회 실패, 카드 결제로 진행 - userId={}", userId, it) }
            .getOrDefault(0)
        return when {
            balance >= details.totalAmount -> payWithPointsUseCase.execute(userId, details)
            balance > 0 -> mixedPaymentUseCase.execute(userId, details, paymentKey)
            else -> cardOnlyPaymentUseCase.execute(userId, details, paymentKey)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PaymentFacade::class.java)
    }
}
