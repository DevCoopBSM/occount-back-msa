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
