package devcoop.occount.payment.application.payment

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.domain.ChargeLogRepository
import devcoop.occount.payment.domain.PaymentLogRepository
import devcoop.occount.payment.domain.exception.InsufficientPointsException
import devcoop.occount.payment.domain.exception.InvalidPaymentRequestException
import devcoop.occount.payment.domain.type.PaymentType
import devcoop.occount.payment.domain.type.TransactionType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.math.min

@Service
class PaymentService(
    private val memberPaymentReadPort: MemberPaymentReadPort,
    private val pointWalletPort: PointWalletPort,
    private val cardPaymentPort: CardPaymentPort,
    private val paymentLogRepository: PaymentLogRepository,
    private val chargeLogRepository: ChargeLogRepository,
) {
    @Transactional
    fun execute(request: PaymentRequest, userId: Long): PaymentResponse {
        validate(request)

        val user = memberPaymentReadPort.getUser(userId)
        return when (request.type) {
            TransactionType.CHARGE -> chargePoints(user, request.charge!!)
            TransactionType.PAYMENT -> payWithPoints(user, request.payment!!)
            TransactionType.MIXED -> payWithPointsAndCard(user, request.payment!!)
        }
    }

    fun getPaymentHistory(userId: Long): List<PaymentLogResult> {
        return paymentLogRepository.findByUserId(userId)
            .map(PaymentLogResult::from)
    }

    fun getPaymentHistoryByDateRange(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<PaymentLogResult> {
        return paymentLogRepository.findByUserIdAndPaymentDateBetween(userId, startDate, endDate)
            .map(PaymentLogResult::from)
    }

    fun getPaymentByType(paymentType: PaymentType): List<PaymentLogResult> {
        return paymentLogRepository.findByPaymentType(paymentType)
            .map(PaymentLogResult::from)
    }

    fun getChargeHistory(userId: Long): List<ChargeLogResult> {
        return chargeLogRepository.findByUserId(userId)
            .map(ChargeLogResult::from)
    }

    fun getChargeHistoryByDateRange(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<ChargeLogResult> {
        return chargeLogRepository.findByUserIdAndChargeDateBetween(userId, startDate, endDate)
            .map(ChargeLogResult::from)
    }

    private fun chargePoints(user: PaymentUserInfo, chargeRequest: ChargeRequest): PaymentResponse {
        val approved = cardPaymentPort.approve(
            amount = chargeRequest.amount,
            items = listOf(ItemCommand.charge(chargeRequest.amount)),
        )
        val pointChange = chargePointBalance(user.userId, chargeRequest.amount)

        chargeLogRepository.save(
            PaymentMapper.toChargeLog(
                user = user,
                chargeAmount = chargeRequest.amount,
                pointChange = pointChange,
                cardResult = approved.card,
                transactionResult = approved.transaction,
            ),
        )

        return PaymentResponse.forCharge(
            chargedAmount = chargeRequest.amount,
            remainingPoints = pointChange.afterBalance,
            approvalNumber = approved.transaction?.approvalNumber.orEmpty(),
            transactionId = approved.transaction?.transactionId.orEmpty(),
        )
    }

    private fun payWithPoints(user: PaymentUserInfo, paymentDetails: PaymentDetails): PaymentResponse {
        val beforeBalance = pointWalletPort.getBalance(user.userId)
        if (beforeBalance < paymentDetails.totalAmount) {
            throw InsufficientPointsException()
        }

        val afterBalance = pointWalletPort.deduct(user.userId, paymentDetails.totalAmount)
        val pointChange = PointBalanceChange(
            beforeBalance = beforeBalance,
            changedAmount = -paymentDetails.totalAmount,
            afterBalance = afterBalance,
        )

        paymentLogRepository.save(
            PaymentMapper.toPointPaymentLog(user, paymentDetails, pointChange),
        )

        return PaymentResponse.forPayment(
            type = PaymentType.POINT,
            totalAmount = paymentDetails.totalAmount,
            paymentAmount = paymentDetails.totalAmount,
            pointsUsed = paymentDetails.totalAmount,
            cardAmount = null,
            remainingPoints = afterBalance,
            approvalNumber = null,
            transactionId = null,
        )
    }

    private fun payWithPointsAndCard(user: PaymentUserInfo, paymentDetails: PaymentDetails): PaymentResponse {
        val beforeBalance = pointWalletPort.getBalance(user.userId)
        val pointsUsed = min(beforeBalance, paymentDetails.totalAmount)
        val cardAmount = paymentDetails.totalAmount - pointsUsed

        if (pointsUsed <= 0 || cardAmount <= 0) {
            throw InvalidPaymentRequestException()
        }

        val approved = cardPaymentPort.approve(
            amount = cardAmount,
            items = paymentDetails.items.map(ItemCommand::from),
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
                paymentDetails = paymentDetails,
                pointChange = pointChange,
                cardResult = approved.card,
                transactionResult = approved.transaction,
            ),
        )

        return PaymentResponse.forPayment(
            type = PaymentType.MIXED,
            totalAmount = paymentDetails.totalAmount,
            paymentAmount = paymentDetails.totalAmount,
            pointsUsed = pointsUsed,
            cardAmount = cardAmount,
            remainingPoints = afterBalance,
            approvalNumber = approved.transaction?.approvalNumber,
            transactionId = approved.transaction?.transactionId,
        )
    }

    private fun chargePointBalance(userId: Long, amount: Int): PointBalanceChange {
        val beforeBalance = pointWalletPort.getBalance(userId)
        val afterBalance = pointWalletPort.charge(userId, amount)
        return PointBalanceChange(
            beforeBalance = beforeBalance,
            changedAmount = amount,
            afterBalance = afterBalance,
        )
    }

    private fun validate(request: PaymentRequest) {
        when (request.type) {
            TransactionType.CHARGE -> {
                if (request.charge == null || request.payment != null) {
                    throw InvalidPaymentRequestException()
                }
            }

            TransactionType.PAYMENT,
            TransactionType.MIXED,
            -> {
                if (request.payment == null || request.charge != null) {
                    throw InvalidPaymentRequestException()
                }
            }
        }
    }
}
