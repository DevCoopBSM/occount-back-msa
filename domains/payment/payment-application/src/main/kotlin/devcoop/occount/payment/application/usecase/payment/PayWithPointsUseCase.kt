package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentMapper
import devcoop.occount.payment.application.shared.PaymentResponse
import devcoop.occount.payment.application.usecase.wallet.deduct.DeductWalletUseCase
import devcoop.occount.payment.domain.payment.PaymentType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PayWithPointsUseCase(
    private val deductWalletUseCase: DeductWalletUseCase,
    private val paymentLogRepository: PaymentLogRepository,
) {
    @Transactional
    fun execute(userId: Long, details: PaymentDetails): PaymentResponse {
        val pointTransaction = deductWalletUseCase.deduct(userId, details.totalAmount)

        paymentLogRepository.save(
            PaymentMapper.toPointPaymentLog(userId, details, pointTransaction),
        )

        return PaymentResponse.forPayment(
            type = PaymentType.POINT,
            totalAmount = details.totalAmount,
            paymentAmount = details.totalAmount,
            pointsUsed = details.totalAmount,
            cardAmount = null,
            remainingPoints = pointTransaction.afterPoint,
            approvalNumber = null,
            transactionId = null,
        )
    }
}
