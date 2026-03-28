package devcoop.occount.payment.application.usecase.mixed

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.MemberPaymentReadPort
import devcoop.occount.payment.application.output.PointWalletPort
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentMapper
import devcoop.occount.payment.application.shared.PaymentResponse
import devcoop.occount.payment.application.shared.PointBalanceChange
import devcoop.occount.payment.domain.PaymentLogRepository
import devcoop.occount.payment.domain.exception.InvalidPaymentRequestException
import devcoop.occount.payment.domain.type.PaymentType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.min

@Service
class MixedPaymentUseCase(
    private val memberPaymentReadPort: MemberPaymentReadPort,
    private val pointWalletPort: PointWalletPort,
    private val cardPaymentPort: CardPaymentPort,
    private val paymentLogRepository: PaymentLogRepository,
) {
    @Transactional
    fun execute(userId: Long, details: PaymentDetails): PaymentResponse {
        val user = memberPaymentReadPort.getUser(userId)

        val beforeBalance = pointWalletPort.getBalance(user.userId)
        val pointsUsed = min(beforeBalance, details.totalAmount)
        val cardAmount = details.totalAmount - pointsUsed

        if (pointsUsed <= 0 || cardAmount <= 0) {
            throw InvalidPaymentRequestException()
        }

        val approved = cardPaymentPort.approve(
            amount = cardAmount,
            items = details.items.map(ItemCommand::from),
        )
        val afterBalance = pointWalletPort.deduct(user.userId, pointsUsed)
        val pointChange = PointBalanceChange(
            beforeBalance = beforeBalance,
            changedAmount = -pointsUsed,
            afterBalance = afterBalance,
        )

        paymentLogRepository.save(
            PaymentMapper.toMixedPaymentLog(
                user = user,
                paymentDetails = details,
                pointChange = pointChange,
                cardResult = approved.card,
                transactionResult = approved.transaction,
            ),
        )

        return PaymentResponse.forPayment(
            type = PaymentType.MIXED,
            totalAmount = details.totalAmount,
            paymentAmount = details.totalAmount,
            pointsUsed = pointsUsed,
            cardAmount = cardAmount,
            remainingPoints = afterBalance,
            approvalNumber = approved.transaction?.approvalNumber,
            transactionId = approved.transaction?.transactionId,
        )
    }
}
