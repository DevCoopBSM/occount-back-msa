package devcoop.occount.payment.application.shared

import devcoop.occount.payment.application.usecase.charge.ChargeUseCase
import devcoop.occount.payment.application.usecase.mixed.MixedPaymentUseCase
import devcoop.occount.payment.application.usecase.payment.PayWithPointsUseCase
import devcoop.occount.payment.domain.type.TransactionType
import org.springframework.stereotype.Service

@Service
class PaymentFacade(
    private val chargeUseCase: ChargeUseCase,
    private val payWithPointsUseCase: PayWithPointsUseCase,
    private val mixedPaymentUseCase: MixedPaymentUseCase,
) {
    fun execute(userId: Long, request: PaymentRequest): PaymentResponse {
        return when (request.type) {
            TransactionType.CHARGE -> chargeUseCase.execute(userId, request.requireCharge())
            TransactionType.PAYMENT -> payWithPointsUseCase.execute(userId, request.requirePayment())
            TransactionType.MIXED -> mixedPaymentUseCase.execute(userId, request.requirePayment())
        }
    }
}
