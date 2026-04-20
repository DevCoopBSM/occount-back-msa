package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompensatedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationFailedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.exception.BusinessBaseException
import devcoop.occount.payment.application.exception.InvalidPaymentRequestException
import devcoop.occount.payment.application.exception.PaymentCancelledException
import devcoop.occount.payment.application.exception.PaymentFailedException
import devcoop.occount.payment.application.exception.PaymentLogNotFoundException
import devcoop.occount.payment.application.exception.PaymentTimeoutException
import devcoop.occount.payment.application.exception.TransactionInProgressException
import devcoop.occount.payment.application.exception.WalletNotFoundException
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.usecase.wallet.refund.RefundWalletPointsUseCase
import devcoop.occount.payment.domain.payment.RefundState
import org.springframework.stereotype.Service

@Service
class CompensateOrderPaymentUseCase(
    private val paymentLogRepository: PaymentLogRepository,
    private val cardPaymentPort: CardPaymentPort,
    private val refundWalletPointsUseCase: RefundWalletPointsUseCase,
    private val eventPublisher: EventPublisher,
) {
    fun compensate(event: OrderPaymentCompensationRequestedEvent) {
        val requiresCardRefund = event.cardAmount > 0
        val requiresPointRefund = event.pointsUsed > 0

        try {
            val paymentLogId = event.paymentLogId ?: throw PaymentLogNotFoundException()
            var paymentLog = paymentLogRepository.findById(paymentLogId)
                ?: throw PaymentLogNotFoundException()

            if (paymentLog.getRefundState() == RefundState.COMPLETED) {
                publishCompensated(event)
                return
            }

            paymentLog.requestRefund(event.orderId)
            if (requiresCardRefund) {
                paymentLog.requestCardRefund()
            }
            if (requiresPointRefund) {
                paymentLog.requestPointRefund()
            }
            paymentLog = paymentLogRepository.save(paymentLog)

            if (requiresCardRefund && paymentLog.getCardRefundState() != RefundState.COMPLETED) {
                val transactionInfo = paymentLog.getTransactionInfo()
                val approvalDate = transactionInfo?.approvalDate()
                    ?: throw InvalidPaymentRequestException()

                cardPaymentPort.refund(
                    transactionId = transactionInfo.transactionId(),
                    approvalNumber = transactionInfo.approvalNumber(),
                    approvalDate = approvalDate,
                    amount = event.cardAmount,
                    kioskId = event.kioskId,
                )

                paymentLog.completeCardRefund()
                paymentLog = paymentLogRepository.save(paymentLog)
            }

            if (requiresPointRefund && paymentLog.getPointRefundState() != RefundState.COMPLETED) {
                val userId = event.userId ?: throw WalletNotFoundException()
                refundWalletPointsUseCase.refund(
                    userId = userId,
                    amount = event.pointsUsed,
                    paymentId = paymentLogId,
                    detailReason = "주문 결제 환불",
                )

                paymentLog.completePointRefund()
                paymentLog = paymentLogRepository.save(paymentLog)
            }

            paymentLog.syncRefundState(
                requiresCardRefund = requiresCardRefund,
                requiresPointRefund = requiresPointRefund,
            )
            paymentLogRepository.save(paymentLog)
            publishCompensated(event)
        } catch (ex: BusinessBaseException) {
            if (ex.isRetryableCompensationError()) {
                throw ex
            }
            publishFailed(event, ex.message ?: "Payment compensation failed")
        }
    }

    private fun publishCompensated(event: OrderPaymentCompensationRequestedEvent) {
        eventPublisher.publish(
            topic = DomainTopics.ORDER_PAYMENT_COMPENSATED,
            key = event.orderId,
            eventType = DomainEventTypes.ORDER_PAYMENT_COMPENSATED,
            payload = OrderPaymentCompensatedEvent(
                orderId = event.orderId,
                userId = event.userId,
            ),
        )
    }

    private fun publishFailed(event: OrderPaymentCompensationRequestedEvent, reason: String) {
        eventPublisher.publish(
            topic = DomainTopics.ORDER_PAYMENT_COMPENSATION_FAILED,
            key = event.orderId,
            eventType = DomainEventTypes.ORDER_PAYMENT_COMPENSATION_FAILED,
            payload = OrderPaymentCompensationFailedEvent(
                orderId = event.orderId,
                userId = event.userId,
                reason = reason,
            ),
        )
    }

    private fun BusinessBaseException.isRetryableCompensationError(): Boolean {
        return this is PaymentFailedException ||
            this is PaymentTimeoutException ||
            this is TransactionInProgressException
    }
}
