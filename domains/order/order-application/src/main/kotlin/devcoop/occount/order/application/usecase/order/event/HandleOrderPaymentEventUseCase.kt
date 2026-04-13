package devcoop.occount.order.application.usecase.order.event

import devcoop.occount.core.common.event.OrderPaymentCompensatedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationFailedEvent
import devcoop.occount.core.common.event.OrderPaymentCompletedEvent
import devcoop.occount.core.common.event.OrderPaymentFailedEvent
import devcoop.occount.order.application.support.OrderLifecycleProcessor
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.domain.order.OrderPaymentResult
import devcoop.occount.order.domain.order.OrderStepStatus
import org.springframework.stereotype.Service

@Service
class HandleOrderPaymentEventUseCase(
    private val orderMutationExecutor: OrderMutationExecutor,
    private val orderLifecycleProcessor: OrderLifecycleProcessor,
) {
    fun applyCompletedPayment(event: OrderPaymentCompletedEvent) {
        val updated = orderMutationExecutor.updateOrder(event.orderId) { current ->
            if (current.paymentStatus != OrderStepStatus.PENDING) {
                return@updateOrder current
            }

            current.copy(
                paymentStatus = OrderStepStatus.SUCCEEDED,
                paymentResult = OrderPaymentResult(
                    paymentLogId = event.paymentLogId,
                    pointsUsed = event.pointsUsed,
                    cardAmount = event.cardAmount,
                    transactionId = event.transactionId,
                    approvalNumber = event.approvalNumber,
                ),
            )
        }

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
    }

    fun applyFailedPayment(event: OrderPaymentFailedEvent) {
        val updated = orderMutationExecutor.updateOrder(event.orderId) { current ->
            if (current.paymentStatus != OrderStepStatus.PENDING) {
                return@updateOrder current
            }

            current.copy(
                paymentStatus = OrderStepStatus.FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        }

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
    }

    fun applyCompensatedPayment(event: OrderPaymentCompensatedEvent) {
        val updated = orderMutationExecutor.updateOrder(event.orderId) { current ->
            if (current.paymentStatus != OrderStepStatus.SUCCEEDED) {
                return@updateOrder current
            }

            current.copy(paymentStatus = OrderStepStatus.COMPENSATED)
        }

        orderLifecycleProcessor.completePendingOrderIfFinal(updated)
    }

    fun applyPaymentCompensationFailure(event: OrderPaymentCompensationFailedEvent) {
        val updated = orderMutationExecutor.updateOrder(event.orderId) { current ->
            if (current.paymentStatus != OrderStepStatus.SUCCEEDED) {
                return@updateOrder current
            }

            current.copy(
                paymentStatus = OrderStepStatus.COMPENSATION_FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        }

        orderLifecycleProcessor.completePendingOrderIfFinal(updated)
    }
}
