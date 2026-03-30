package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.payment.application.output.WalletPort
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentMapper
import devcoop.occount.payment.application.shared.PaymentResponse
import devcoop.occount.payment.application.shared.PointBalanceChange
import devcoop.occount.payment.domain.PaymentLogRepository
import devcoop.occount.payment.domain.exception.InsufficientPointsException
import devcoop.occount.payment.domain.type.PaymentType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PayWithPointsUseCase(
    private val pointWalletPort: WalletPort,
    private val paymentLogRepository: PaymentLogRepository,
) {
    @Transactional
    fun execute(userId: Long, details: PaymentDetails): PaymentResponse {
        val beforeBalance = pointWalletPort.getBalance(userId)
        if (beforeBalance < details.totalAmount) {
            throw InsufficientPointsException()
        }

        val afterBalance = pointWalletPort.deduct(userId, details.totalAmount)
        val pointChange = PointBalanceChange(
            beforeBalance = beforeBalance,
            changedAmount = -details.totalAmount,
            afterBalance = afterBalance,
        )

        paymentLogRepository.save(
            PaymentMapper.toPointPaymentLog(userId, details, pointChange),
        )

        return PaymentResponse.forPayment(
            type = PaymentType.POINT,
            totalAmount = details.totalAmount,
            paymentAmount = details.totalAmount,
            pointsUsed = details.totalAmount,
            cardAmount = null,
            remainingPoints = afterBalance,
            approvalNumber = null,
            transactionId = null,
        )
    }
}
