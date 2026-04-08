package devcoop.occount.order.domain.order

import java.time.Instant

data class OrderAggregate(
    val orderId: String,
    val userId: Long,
    val lines: List<OrderLine>,
    val payment: OrderPayment,
    val status: OrderStatus = OrderStatus.PENDING,
    val paymentStatus: OrderStepStatus = OrderStepStatus.PENDING,
    val stockStatus: OrderStepStatus = OrderStepStatus.PENDING,
    val cancelRequested: Boolean = false,
    val failureReason: String? = null,
    val expiresAt: Instant,
    val paymentResult: OrderPaymentResult = OrderPaymentResult(),
    val paymentCompensationRequested: Boolean = false,
    val stockCompensationRequested: Boolean = false,
    val version: Long = 0L,
) {
    fun reconcile(): OrderAggregate {
        if (paymentStatus == OrderStepStatus.COMPENSATION_FAILED ||
            stockStatus == OrderStepStatus.COMPENSATION_FAILED
        ) {
            return copy(status = OrderStatus.COMPENSATION_FAILED)
        }

        if (cancelRequested) {
            if (paymentStatus.isPending() || stockStatus.isPending()) {
                return copy(
                    status = if (status == OrderStatus.TIMED_OUT) {
                        OrderStatus.TIMED_OUT
                    } else {
                        OrderStatus.CANCEL_REQUESTED
                    },
                )
            }

            if (paymentStatus.requiresCompensation() || stockStatus.requiresCompensation()) {
                return copy(
                    status = if (status == OrderStatus.TIMED_OUT) {
                        OrderStatus.TIMED_OUT
                    } else {
                        OrderStatus.COMPENSATING
                    },
                )
            }

            if (paymentStatus.isCompensationResolved() && stockStatus.isCompensationResolved()) {
                return copy(status = OrderStatus.CANCELLED)
            }
        }

        if (paymentStatus == OrderStepStatus.SUCCEEDED &&
            stockStatus == OrderStepStatus.SUCCEEDED
        ) {
            return copy(status = OrderStatus.COMPLETED)
        }

        val hasFailure = paymentStatus == OrderStepStatus.FAILED ||
            stockStatus == OrderStepStatus.FAILED
        if (hasFailure) {
            if (paymentStatus.isPending() || stockStatus.isPending()) {
                return copy(status = OrderStatus.PROCESSING)
            }

            if (paymentStatus.requiresCompensation() || stockStatus.requiresCompensation()) {
                return copy(status = OrderStatus.COMPENSATING)
            }

            return copy(status = OrderStatus.FAILED)
        }

        return copy(status = OrderStatus.PROCESSING)
    }

    fun shouldCompensate(): Boolean =
        cancelRequested ||
            paymentStatus == OrderStepStatus.FAILED ||
            stockStatus == OrderStepStatus.FAILED ||
            status == OrderStatus.TIMED_OUT

    fun shouldRequestPaymentCompensation(): Boolean =
        shouldCompensate() &&
            paymentStatus == OrderStepStatus.SUCCEEDED &&
            !paymentCompensationRequested

    fun shouldRequestStockCompensation(): Boolean =
        shouldCompensate() &&
            stockStatus == OrderStepStatus.SUCCEEDED &&
            !stockCompensationRequested
}
