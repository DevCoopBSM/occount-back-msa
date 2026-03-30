package devcoop.occount.payment.application.usecase.mixed

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.exception.InvalidPaymentRequestException
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.query.wallet.GetWalletBalanceQueryService
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentMapper
import devcoop.occount.payment.application.shared.PaymentResponse
import devcoop.occount.payment.application.usecase.wallet.deduct.DeductWalletUseCase
import devcoop.occount.payment.domain.payment.PaymentType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.min

@Service
class MixedPaymentUseCase(
    private val getWalletBalanceQueryService: GetWalletBalanceQueryService,
    private val deductWalletUseCase: DeductWalletUseCase,
    private val cardPaymentPort: CardPaymentPort,
    private val paymentLogRepository: PaymentLogRepository,
) {
    @Transactional
    fun execute(userId: Long, details: PaymentDetails): PaymentResponse {
        val beforePoint = getWalletBalanceQueryService.getBalance(userId)
        val pointsUsed = min(beforePoint, details.totalAmount)
        val cardAmount = details.totalAmount - pointsUsed

        if (pointsUsed <= 0 || cardAmount <= 0) {
            throw InvalidPaymentRequestException()
        }

        val approved = cardPaymentPort.approve(
            amount = cardAmount,
            items = details.items.map(ItemCommand::from),
        )
        val pointTransaction = deductWalletUseCase.deduct(userId, pointsUsed)

        paymentLogRepository.save(
            PaymentMapper.toMixedPaymentLog(
                userId = userId,
                paymentDetails = details,
                cardResult = approved.card,
                transactionResult = approved.transaction,
                pointTransaction = pointTransaction,
            ),
        )

        return PaymentResponse.forPayment(
            type = PaymentType.MIXED,
            totalAmount = details.totalAmount,
            paymentAmount = details.totalAmount,
            pointsUsed = pointsUsed,
            cardAmount = cardAmount,
            remainingPoints = pointTransaction.afterPoint,
            approvalNumber = approved.transaction?.approvalNumber,
            transactionId = approved.transaction?.transactionId,
        )
    }
}
