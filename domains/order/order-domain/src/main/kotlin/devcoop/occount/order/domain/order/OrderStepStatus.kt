package devcoop.occount.order.domain.order

enum class OrderStepStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    COMPENSATED,
    COMPENSATION_FAILED,
}

fun OrderStepStatus.isPending(): Boolean = this == OrderStepStatus.PENDING
fun OrderStepStatus.requiresCompensation(): Boolean = this == OrderStepStatus.SUCCEEDED
fun OrderStepStatus.isCompensationResolved(): Boolean =
    this == OrderStepStatus.COMPENSATED ||
        this == OrderStepStatus.PENDING ||
        this == OrderStepStatus.FAILED
