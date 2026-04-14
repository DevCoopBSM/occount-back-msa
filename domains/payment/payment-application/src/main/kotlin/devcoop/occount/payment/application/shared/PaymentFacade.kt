package devcoop.occount.payment.application.shared

import devcoop.occount.payment.application.usecase.payment.CardOnlyPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.MixedPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.PayWithPointsUseCase
import devcoop.occount.payment.domain.payment.TransactionType
import org.springframework.stereotype.Service

@Service
class PaymentFacade(
    private val payWithPointsUseCase: PayWithPointsUseCase,
    private val mixedPaymentUseCase: MixedPaymentUseCase,
    private val cardOnlyPaymentUseCase: CardOnlyPaymentUseCase,
) {
    fun execute(userId: Long?, request: PaymentRequest): PaymentResponse {
        return when (request.type) {
            TransactionType.PAYMENT -> payWithPointsUseCase.execute(requireNotNull(userId), request.requirePayment())
            TransactionType.MIXED -> mixedPaymentUseCase.execute(requireNotNull(userId), request.requirePayment())
            TransactionType.CARD -> cardOnlyPaymentUseCase.execute(userId, request.requirePayment())
        }
    }
}
