package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompletedEvent
import devcoop.occount.core.common.event.OrderPaymentFailedEvent
import devcoop.occount.core.common.event.OrderPaymentRequestedEvent
import devcoop.occount.payment.application.exception.PaymentCancelledException
import devcoop.occount.payment.application.output.OrderPaymentExecutionRepository
import devcoop.occount.payment.application.output.OrderPaymentExecutionStartResult
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentFacade
import devcoop.occount.payment.application.shared.PaymentItem
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProcessOrderPaymentUseCase(
    private val paymentFacade: PaymentFacade,
    private val orderPaymentExecutionRepository: OrderPaymentExecutionRepository,
    private val eventPublisher: EventPublisher,
) {
    fun process(event: OrderPaymentRequestedEvent) {
        when (orderPaymentExecutionRepository.startProcessing(event.orderId)) {
            OrderPaymentExecutionStartResult.CANCELLED_BEFORE_START -> {
                log.info("결제 요청 스킵 - 선행 취소 상태 orderId={}", event.orderId)
                orderPaymentExecutionRepository.markCancelled(event.orderId)
                publishFailed(event, PaymentCancelledException().message ?: "Payment cancelled before approval started")
                return
            }

            OrderPaymentExecutionStartResult.DUPLICATE -> {
                log.info("결제 요청 중복 감지 - orderId={}", event.orderId)
                return
            }

            OrderPaymentExecutionStartResult.STARTED -> Unit
        }

        if (orderPaymentExecutionRepository.isCancellationRequested(event.orderId)) {
            log.info("결제 시작 직전 취소 감지 - orderId={}", event.orderId)
            orderPaymentExecutionRepository.markCancelled(event.orderId)
            publishFailed(event, PaymentCancelledException().message ?: "Payment cancelled before approval started")
            return
        }

        try {
            val result = paymentFacade.execute(
                userId = event.userId,
                kioskId = event.kioskId,
                details = PaymentDetails(
                    items = event.items.map { item ->
                        PaymentItem(
                            itemId = item.itemId.toString(),
                            itemName = item.itemName,
                            itemPrice = item.itemPrice,
                            quantity = item.quantity,
                            totalPrice = item.totalPrice,
                        )
                    },
                    totalAmount = event.payment.totalAmount,
                ),
                paymentKey = event.orderId,
            )
            orderPaymentExecutionRepository.markCompleted(event.orderId)

            eventPublisher.publish(
                topic = DomainTopics.ORDER_PAYMENT_COMPLETED,
                key = event.orderId,
                eventType = DomainEventTypes.ORDER_PAYMENT_COMPLETED,
                payload = OrderPaymentCompletedEvent(
                    orderId = event.orderId,
                    userId = event.userId,
                    paymentLogId = result.paymentLogId ?: 0L,
                    pointsUsed = result.pointsUsed ?: 0,
                    cardAmount = result.cardAmount ?: 0,
                    totalAmount = result.totalAmount ?: event.payment.totalAmount,
                    transactionId = result.transactionId,
                    approvalNumber = result.approvalNumber,
                ),
            )
        } catch (ex: PaymentCancelledException) {
            orderPaymentExecutionRepository.markCancelled(event.orderId)
            publishFailed(event, ex.message ?: "Payment cancelled")
        } catch (ex: Exception) {
            orderPaymentExecutionRepository.markFailed(event.orderId)
            publishFailed(event, ex.message ?: "Payment processing failed")
        }
    }

    private fun publishFailed(event: OrderPaymentRequestedEvent, reason: String) {
        eventPublisher.publish(
            topic = DomainTopics.ORDER_PAYMENT_FAILED,
            key = event.orderId,
            eventType = DomainEventTypes.ORDER_PAYMENT_FAILED,
            payload = OrderPaymentFailedEvent(
                orderId = event.orderId,
                userId = event.userId,
                reason = reason,
            ),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(ProcessOrderPaymentUseCase::class.java)
    }
}
