package devcoop.occount.order.application.usecase.order.event

import devcoop.occount.core.common.event.OrderPaymentCompensatedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationFailedEvent
import devcoop.occount.core.common.event.OrderPaymentCompletedEvent
import devcoop.occount.core.common.event.OrderPaymentFailedEvent
import devcoop.occount.order.application.support.OrderFailureReasonSanitizer
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
    fun applyCompletedPayment(event: OrderPaymentCompletedEvent, recordConsumption: () -> Unit) {
        val updated = orderMutationExecutor.updateOrderIdempotently(
            orderId = event.orderId,
            recordConsumption = recordConsumption,
        ) { current ->
            if (current.paymentStatus != OrderStepStatus.PENDING) return@updateOrderIdempotently current
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
        } ?: return

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
    }

    fun applyFailedPayment(event: OrderPaymentFailedEvent, recordConsumption: () -> Unit) {
        val updated = orderMutationExecutor.updateOrderIdempotently(
            orderId = event.orderId,
            recordConsumption = recordConsumption,
        ) { current ->
            if (current.paymentStatus != OrderStepStatus.PENDING) return@updateOrderIdempotently current
            current.copy(
                paymentStatus = OrderStepStatus.FAILED,
                failureReason = current.failureReason ?: OrderFailureReasonSanitizer.sanitize(event.reason),
            )
        } ?: return

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
    }

    fun applyCompensatedPayment(event: OrderPaymentCompensatedEvent, recordConsumption: () -> Unit) {
        val updated = orderMutationExecutor.updateOrderIdempotently(
            orderId = event.orderId,
            recordConsumption = recordConsumption,
        ) { current ->
            if (current.paymentStatus != OrderStepStatus.SUCCEEDED) return@updateOrderIdempotently current
            current.copy(paymentStatus = OrderStepStatus.COMPENSATED)
        } ?: return

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
    }

    fun applyPaymentCompensationFailure(event: OrderPaymentCompensationFailedEvent, recordConsumption: () -> Unit) {
        val updated = orderMutationExecutor.updateOrderIdempotently(
            orderId = event.orderId,
            recordConsumption = recordConsumption,
        ) { current ->
            if (current.paymentStatus != OrderStepStatus.SUCCEEDED) return@updateOrderIdempotently current
            current.copy(
                paymentStatus = OrderStepStatus.COMPENSATION_FAILED,
                failureReason = current.failureReason ?: OrderFailureReasonSanitizer.sanitize(event.reason),
            )
        } ?: return

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
    }
}
