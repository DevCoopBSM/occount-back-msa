package devcoop.occount.payment.application.usecase.charge

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.MemberPaymentReadPort
import devcoop.occount.payment.application.output.PointWalletPort
import devcoop.occount.payment.application.shared.ChargeRequest
import devcoop.occount.payment.application.shared.PaymentMapper
import devcoop.occount.payment.application.shared.PaymentResponse
import devcoop.occount.payment.application.shared.PointBalanceChange
import devcoop.occount.payment.domain.ChargeLogRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChargeUseCase(
    private val memberPaymentReadPort: MemberPaymentReadPort,
    private val pointWalletPort: PointWalletPort,
    private val cardPaymentPort: CardPaymentPort,
    private val chargeLogRepository: ChargeLogRepository,
) {
    @Transactional
    fun execute(userId: Long, request: ChargeRequest): PaymentResponse {
        val user = memberPaymentReadPort.getUser(userId)

        val approved = cardPaymentPort.approve(
            amount = request.amount,
            items = listOf(ItemCommand.charge(request.amount)),
        )

        val beforeBalance = pointWalletPort.getBalance(user.userId)
        val afterBalance = pointWalletPort.charge(user.userId, request.amount)
        val pointChange = PointBalanceChange(
            beforeBalance = beforeBalance,
            changedAmount = request.amount,
            afterBalance = afterBalance,
        )

        chargeLogRepository.save(
            PaymentMapper.toChargeLog(
                user = user,
                chargeAmount = request.amount,
                pointChange = pointChange,
                cardResult = approved.card,
                transactionResult = approved.transaction,
            ),
        )

        return PaymentResponse.forCharge(
            chargedAmount = request.amount,
            remainingPoints = pointChange.afterBalance,
            approvalNumber = approved.transaction?.approvalNumber.orEmpty(),
            transactionId = approved.transaction?.transactionId.orEmpty(),
        )
    }
}
