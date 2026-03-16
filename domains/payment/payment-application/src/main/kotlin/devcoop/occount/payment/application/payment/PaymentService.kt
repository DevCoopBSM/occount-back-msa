package devcoop.occount.payment.application.payment

import devcoop.occount.payment.application.dto.request.ProductInfo
import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.exception.InsufficientPointsException
import devcoop.occount.payment.domain.exception.InvalidPaymentRequestException
import devcoop.occount.payment.domain.type.EventType
import devcoop.occount.payment.domain.type.PaymentType
import devcoop.occount.payment.domain.type.RefundState
import devcoop.occount.payment.domain.type.TransactionType
import devcoop.occount.payment.domain.vo.PointTransaction
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

    fun getPaymentHistory(userId: Long): List<PaymentLog> {
        return paymentLogRepository.findByUserId(userId)
    }

    fun getPaymentHistoryByDateRange(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<PaymentLog> {
        return paymentLogRepository.findByUserIdAndPaymentDateBetween(userId, startDate, endDate)
    }

    fun getPaymentByType(paymentType: PaymentType): List<PaymentLog> {
        return paymentLogRepository.findByPaymentType(paymentType)
    }

    fun getChargeHistory(userId: Long): List<ChargeLog> {
        return chargeLogRepository.findByUserId(userId)
    }

    fun getChargeHistoryByDateRange(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<ChargeLog> {
        return chargeLogRepository.findByUserIdAndChargeDateBetween(userId, startDate, endDate)
    }

    private fun chargePoints(user: PaymentUserInfo, chargeRequest: ChargeRequest): PaymentResponse {
        val approved = cardPaymentPort.approve(
            amount = chargeRequest.amount,
            products = listOf(toChargeProduct(chargeRequest.amount)),
        )
        val pointChange = chargePointBalance(user.userId, chargeRequest.amount)

        chargeLogRepository.save(
            ChargeLog(
                userId = user.userId,
                chargeAmount = chargeRequest.amount,
                pointTransaction = pointTransaction(pointChange),
                cardInfo = approved.card?.let(::toDomainCardInfo),
                transactionInfo = approved.transaction?.let(::toDomainTransactionInfo),
                managedEmail = user.email,
                refundState = RefundState.NONE,
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
            PaymentLog(
                userId = user.userId,
                paymentType = PaymentType.POINT,
                totalAmount = paymentDetails.totalAmount,
                pointTransaction = pointTransaction(pointChange),
                managedEmail = user.email,
                eventType = EventType.NONE,
            ),
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
            products = paymentDetails.items.map(::toProductInfo),
        )
        val afterBalance = pointWalletPort.deduct(user.userId, pointsUsed)
        val pointChange = PointBalanceChange(
            beforeBalance = beforeBalance,
            changedAmount = -pointsUsed,
            afterBalance = afterBalance,
        )

        paymentLogRepository.save(
            PaymentLog(
                userId = user.userId,
                paymentType = PaymentType.MIXED,
                totalAmount = paymentDetails.totalAmount,
                pointTransaction = pointTransaction(pointChange),
                cardInfo = approved.card?.let(::toDomainCardInfo),
                transactionInfo = approved.transaction?.let(::toDomainTransactionInfo),
                managedEmail = user.email,
                eventType = EventType.NONE,
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

    private fun pointTransaction(change: PointBalanceChange): PointTransaction {
        return PointTransaction(
            beforePoint = change.beforeBalance,
            transactionPoint = change.changedAmount,
            afterPoint = change.afterBalance,
        )
    }

    private fun toChargeProduct(amount: Int): ProductInfo {
        return toProductInfo(
            PaymentItem(
                itemId = "CHARGE",
                itemName = "포인트 충전",
                itemPrice = amount,
                quantity = 1,
                totalPrice = amount,
            ),
        )
    }

    private fun toProductInfo(item: PaymentItem): ProductInfo {
        return ProductInfo(
            name = item.itemName,
            price = item.itemPrice,
            quantity = item.quantity,
            total = item.totalPrice,
        )
    }

    private fun toDomainTransactionInfo(
        dto: devcoop.occount.payment.application.dto.response.TransactionInfo,
    ): devcoop.occount.payment.domain.vo.TransactionInfo {
        return devcoop.occount.payment.domain.vo.TransactionInfo(
            transactionId = dto.transactionId,
            approvalNumber = dto.approvalNumber,
            cardNumber = dto.cardNumber,
            amount = dto.amount,
            installmentMonths = dto.installmentMonths,
            approvalDate = dto.approvalDate,
            approvalTime = dto.approvalTime,
            terminalId = dto.terminalId,
            merchantNumber = dto.merchantNumber,
        )
    }

    private fun toDomainCardInfo(
        dto: devcoop.occount.payment.application.dto.response.CardInfo,
    ): devcoop.occount.payment.domain.vo.CardInfo {
        return devcoop.occount.payment.domain.vo.CardInfo(
            issuerCode = dto.issuerCode,
            issuerName = dto.issuerName,
            acquirerCode = dto.acquirerCode,
            acquirerName = dto.acquirerName,
            cardType = dto.cardType,
            cardCategory = dto.cardCategory,
            cardName = dto.cardName,
            cardBrand = dto.cardBrand,
        )
    }
}
