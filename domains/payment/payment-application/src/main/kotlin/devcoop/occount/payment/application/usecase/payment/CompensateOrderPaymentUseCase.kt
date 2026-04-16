package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompensatedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationFailedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.payment.application.exception.PaymentLogNotFoundException
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.usecase.wallet.charge.ChargeWalletRequest
import devcoop.occount.payment.application.usecase.wallet.charge.ChargeWalletUseCase
import devcoop.occount.payment.domain.wallet.ChargeReason
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompensateOrderPaymentUseCase(
    private val paymentLogRepository: PaymentLogRepository,
    private val chargeWalletUseCase: ChargeWalletUseCase,
    private val cardPaymentPort: CardPaymentPort,
    private val eventPublisher: EventPublisher,
) {
    @Transactional
    fun compensate(event: OrderPaymentCompensationRequestedEvent) {
        try {
            val paymentLog = event.paymentLogId
                ?.let(paymentLogRepository::findById)
                ?: throw PaymentLogNotFoundException()

            paymentLog.requestRefund(event.orderId)
            paymentLogRepository.save(paymentLog)

            if (event.pointsUsed > 0) {
                chargeWalletUseCase.charge(
                    ChargeWalletRequest(
                        userId = requireNotNull(event.userId) { "포인트 환불은 회원 주문에서만 발생합니다" },
                        amount = event.pointsUsed,
                        reason = ChargeReason.ORDER_CANCELLATION,
                    ),
                )
            }

            if (event.cardAmount > 0) {
                cardPaymentPort.cancel(
                    transactionId = paymentLog.getTransactionInfo()?.transactionId(),
                    approvalNumber = paymentLog.getTransactionInfo()?.approvalNumber(),
                    approvalDate = paymentLog.getTransactionInfo()?.approvalDate() ?: "",
                    amount = event.cardAmount,
                )
            }

            paymentLog.completeRefund()
            paymentLogRepository.save(paymentLog)

            eventPublisher.publish(
                topic = DomainTopics.ORDER_PAYMENT_COMPENSATED,
                key = event.orderId,
                eventType = DomainEventTypes.ORDER_PAYMENT_COMPENSATED,
                payload = OrderPaymentCompensatedEvent(
                    orderId = event.orderId,
                    userId = event.userId,
                ),
            )
        } catch (ex: Exception) {
            eventPublisher.publish(
                topic = DomainTopics.ORDER_PAYMENT_COMPENSATION_FAILED,
                key = event.orderId,
                eventType = DomainEventTypes.ORDER_PAYMENT_COMPENSATION_FAILED,
                payload = OrderPaymentCompensationFailedEvent(
                    orderId = event.orderId,
                    userId = event.userId,
                    reason = ex.message ?: "Payment compensation failed",
                ),
            )
        }
    }
}
