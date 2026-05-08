package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.PaymentCompletedEvent
import devcoop.occount.core.common.event.PaymentFailedEvent
import devcoop.occount.core.common.event.OrderPaymentRequestedEvent
import devcoop.occount.payment.application.exception.DuplicateEventException
import devcoop.occount.payment.application.exception.PaymentCancelledException
import devcoop.occount.payment.application.output.OrderPaymentExecutionRepository
import devcoop.occount.payment.application.output.OrderPaymentExecutionStartResult
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentFacade
import devcoop.occount.payment.application.shared.PaymentItem
import devcoop.occount.payment.application.shared.PaymentResponse
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class ExecuteVanPaymentUseCase(
    private val paymentFacade: PaymentFacade,
    private val orderPaymentExecutionRepository: OrderPaymentExecutionRepository,
    private val eventPublisher: EventPublisher,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager)

    fun execute(event: OrderPaymentRequestedEvent, recordConsumption: () -> Unit = {}) {
        log.info(
            "결제 처리 시작 - orderId={} kioskId={} userId={} amount={}",
            event.orderId,
            event.kioskId,
            event.userId,
            event.payment.totalAmount,
        )

        val startResult = try {
            transactionTemplate.execute {
                recordConsumption()
                orderPaymentExecutionRepository.startProcessing(event.orderId)
            }
        } catch (_: DuplicateEventException) {
            log.info("결제 요청 이벤트 중복 스킵 - orderId={}", event.orderId)
            return
        } catch (_: DataIntegrityViolationException) {
            log.info("결제 요청 이벤트 중복 스킵(PK 충돌) - orderId={}", event.orderId)
            return
        }

        when (startResult) {
            OrderPaymentExecutionStartResult.CANCELLED_BEFORE_START -> {
                log.info("결제 요청 스킵 - 선행 취소 상태 orderId={}", event.orderId)
                completeAsCancelled(event, PaymentCancelledException().message ?: "Payment cancelled before approval started")
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
            completeAsCancelled(event, PaymentCancelledException().message ?: "Payment cancelled before approval started")
            return
        }

        val result = try {
            paymentFacade.execute(
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
        } catch (ex: PaymentCancelledException) {
            log.warn("결제 처리 취소 - orderId={} reason={}", event.orderId, ex.message, ex)
            completeAsCancelled(event, ex.message ?: "Payment cancelled")
            return
        } catch (ex: Exception) {
            log.error("결제 처리 실패 - orderId={} message={}", event.orderId, ex.message, ex)
            completeAsFailed(event, ex.message ?: "Payment processing failed")
            return
        }

        completeAsSuccess(event, result)
    }

    private fun completeAsSuccess(event: OrderPaymentRequestedEvent, result: PaymentResponse) {
        transactionTemplate.executeWithoutResult {
            orderPaymentExecutionRepository.markCompleted(event.orderId)
            eventPublisher.publish(
                topic = DomainTopics.PAYMENT_COMPLETED,
                key = event.orderId.toString(),
                eventType = DomainEventTypes.PAYMENT_COMPLETED,
                payload = PaymentCompletedEvent(
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
        }
        log.info(
            "결제 처리 성공 - orderId={} paymentLogId={} approvalNumber={} transactionId={}",
            event.orderId,
            result.paymentLogId,
            result.approvalNumber,
            result.transactionId,
        )
    }

    private fun completeAsCancelled(event: OrderPaymentRequestedEvent, reason: String) {
        transactionTemplate.executeWithoutResult {
            orderPaymentExecutionRepository.markCancelled(event.orderId)
            publishFailed(event, reason)
        }
    }

    private fun completeAsFailed(event: OrderPaymentRequestedEvent, reason: String) {
        transactionTemplate.executeWithoutResult {
            orderPaymentExecutionRepository.markFailed(event.orderId)
            publishFailed(event, reason)
        }
    }

    private fun publishFailed(event: OrderPaymentRequestedEvent, reason: String) {
        eventPublisher.publish(
            topic = DomainTopics.PAYMENT_FAILED,
            key = event.orderId.toString(),
            eventType = DomainEventTypes.PAYMENT_FAILED,
            payload = PaymentFailedEvent(
                orderId = event.orderId,
                userId = event.userId,
                reason = reason,
            ),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExecuteVanPaymentUseCase::class.java)
    }
}
