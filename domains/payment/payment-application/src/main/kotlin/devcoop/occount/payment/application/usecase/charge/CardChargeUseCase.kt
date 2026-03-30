package devcoop.occount.payment.application.usecase.charge

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.shared.PaymentResponse
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.PaymentType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CardChargeUseCase(
    private val cardPaymentPort: CardPaymentPort,
    private val paymentLogRepository: PaymentLogRepository,
) {
    @Transactional
    fun execute(userId: Long, amount: Int): PaymentResponse {
        val approved = cardPaymentPort.approve(
            amount = amount,
            items = listOf(ItemCommand.charge(amount)),
        )

        val paymentLog = paymentLogRepository.save(
            PaymentLog(
                userId = userId,
                paymentType = PaymentType.CARD,
                totalAmount = amount,
                cardInfo = approved.card?.let(CardResult::toDomain),
                transactionInfo = approved.transaction?.let(TransactionResult::toDomain),
            )
        )

        return PaymentResponse.forCharge(
            paymentLogId = paymentLog.getPaymentId(),
            chargedAmount = amount,
            approvalNumber = approved.transaction?.approvalNumber,
            transactionId = approved.transaction?.transactionId,
        )
    }
}
