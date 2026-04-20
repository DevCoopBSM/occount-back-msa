package devcoop.occount.order.domain.order

import java.time.Instant

data class OrderAggregate(
    val orderId: String,
    val userId: Long?,
    val requestedLines: List<RequestedOrderLine> = emptyList(),
    val lines: List<OrderLine> = emptyList(),
    val payment: OrderPayment,
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentStatus: OrderStepStatus = OrderStepStatus.PENDING,
    val stockStatus: OrderStepStatus = OrderStepStatus.PENDING,
    val cancelRequested: Boolean = false,
    val failureReason: String? = null,
    val kioskId: String,
    val expiresAt: Instant,
    val paymentResult: OrderPaymentResult = OrderPaymentResult(),
    val paymentRequested: Boolean = false,
    val paymentCancellationRequested: Boolean = false,
    val paymentCompensationRequested: Boolean = false,
    val stockCompensationRequested: Boolean = false,
) {
    fun reconcileStatus(): OrderAggregate {
        if (hasCompensationFailure()) {
            return copy(status = OrderStatus.COMPENSATION_FAILED)
        }

        deriveCancellationStatus()?.let { cancellationStatus ->
            return copy(status = cancellationStatus)
        }

        if (isFullyCompleted()) {
            return copy(status = OrderStatus.COMPLETED)
        }

        deriveFailureStatus()?.let { failureStatus ->
            return copy(status = failureStatus)
        }

        return copy(status = OrderStatus.PROCESSING)
    }

    fun requiresCompensation(): Boolean =
        cancelRequested ||
            paymentStatus == OrderStepStatus.FAILED ||
            stockStatus == OrderStepStatus.FAILED ||
            status == OrderStatus.TIMED_OUT

    fun shouldRequestPaymentCompensation(): Boolean =
        requiresCompensation() &&
            paymentStatus == OrderStepStatus.SUCCEEDED &&
            !paymentCompensationRequested

    fun shouldRequestPendingPaymentCancellation(): Boolean =
        cancelRequested &&
            paymentRequested &&
            paymentStatus == OrderStepStatus.PENDING &&
            !paymentCancellationRequested

    fun shouldRequestStockCompensation(): Boolean =
        requiresCompensation() &&
            stockStatus == OrderStepStatus.SUCCEEDED &&
            !stockCompensationRequested

    fun isReadyForPaymentRequest(): Boolean =
        status == OrderStatus.PROCESSING &&
            stockStatus == OrderStepStatus.SUCCEEDED &&
            paymentStatus == OrderStepStatus.PENDING &&
            lines.isNotEmpty() &&
            payment.totalAmount > 0 &&
            !cancelRequested &&
            !paymentRequested

    private fun hasCompensationFailure(): Boolean {
        return paymentStatus == OrderStepStatus.COMPENSATION_FAILED ||
            stockStatus == OrderStepStatus.COMPENSATION_FAILED
    }

    private fun deriveCancellationStatus(): OrderStatus? {
        if (!cancelRequested) {
            return null
        }

        if (hasPendingInFlightStep()) {
            return timeoutAwareStatus(OrderStatus.CANCEL_REQUESTED)
        }

        if (hasSucceededStepNeedingCompensation()) {
            return timeoutAwareStatus(OrderStatus.COMPENSATING)
        }

        if (isCompensationResolved()) {
            return OrderStatus.CANCELLED
        }

        return null
    }

    private fun deriveFailureStatus(): OrderStatus? {
        if (!hasAnyStepFailure()) {
            return null
        }

        if (hasSucceededStepNeedingCompensation()) {
            return OrderStatus.COMPENSATING
        }

        if (hasPendingInFlightStep()) {
            return OrderStatus.PROCESSING
        }

        return OrderStatus.FAILED
    }

    private fun timeoutAwareStatus(defaultStatus: OrderStatus): OrderStatus {
        return if (status == OrderStatus.TIMED_OUT) {
            OrderStatus.TIMED_OUT
        } else {
            defaultStatus
        }
    }

    private fun isFullyCompleted(): Boolean {
        return paymentStatus == OrderStepStatus.SUCCEEDED &&
            stockStatus == OrderStepStatus.SUCCEEDED
    }

    private fun hasAnyStepFailure(): Boolean {
        return paymentStatus == OrderStepStatus.FAILED ||
            stockStatus == OrderStepStatus.FAILED
    }

    private fun hasPendingInFlightStep(): Boolean {
        return stockStatus.isPending() ||
            (paymentStatus.isPending() && paymentRequested)
    }

    private fun hasSucceededStepNeedingCompensation(): Boolean {
        return paymentStatus.requiresCompensation() || stockStatus.requiresCompensation()
    }

    private fun isCompensationResolved(): Boolean {
        return paymentStatus.isCompensationResolved() &&
            stockStatus.isCompensationResolved()
    }
}
