package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentMapper
import devcoop.occount.payment.application.shared.PaymentResponse
import devcoop.occount.payment.domain.payment.PaymentType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CardOnlyPaymentUseCase(
    private val cardPaymentPort: CardPaymentPort,
    private val paymentLogRepository: PaymentLogRepository,
) {
    @Transactional
    fun execute(userId: Long?, details: PaymentDetails): PaymentResponse {
        val approved = cardPaymentPort.approve(
            amount = details.totalAmount,
            items = details.items.map(ItemCommand.Companion::from),
        )

        val paymentLog = paymentLogRepository.save(
            PaymentMapper.toCardPaymentLog(
                userId = userId,
                paymentDetails = details,
                cardResult = approved.card,
                transactionResult = approved.transaction,
            ),
        )

        return PaymentResponse.forPayment(
            type = PaymentType.CARD,
            totalAmount = details.totalAmount,
            paymentAmount = details.totalAmount,
            pointsUsed = 0,
            cardAmount = details.totalAmount,
            remainingPoints = 0,
            approvalNumber = approved.transaction?.approvalNumber,
            transactionId = approved.transaction?.transactionId,
            paymentLogId = paymentLog.getPaymentId(),
        )
    }
}
