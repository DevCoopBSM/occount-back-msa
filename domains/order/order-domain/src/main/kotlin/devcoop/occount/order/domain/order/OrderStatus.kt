package devcoop.occount.order.domain.order

enum class OrderStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCEL_REQUESTED,
    COMPENSATING,
    CANCELLED,
    COMPENSATION_FAILED,
    TIMED_OUT,
}

fun OrderStatus.canCancel(): Boolean =
    this == OrderStatus.PENDING ||
        this == OrderStatus.PROCESSING ||
        this == OrderStatus.CANCEL_REQUESTED ||
        this == OrderStatus.COMPENSATING ||
        this == OrderStatus.TIMED_OUT

fun OrderStatus.isFinalForClient(): Boolean =
    this == OrderStatus.COMPLETED ||
        this == OrderStatus.FAILED ||
        this == OrderStatus.CANCELLED ||
        this == OrderStatus.COMPENSATION_FAILED ||
        this == OrderStatus.TIMED_OUT
