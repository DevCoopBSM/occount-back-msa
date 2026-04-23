package devcoop.occount.order.application.shared

enum class OrderStreamEventType {
    ORDER_ACCEPTED,
    STOCK_CONFIRMED,
    PAYMENT_REQUESTED,
    COMPLETED,
    FAILED,
    CANCEL_REQUESTED,
    COMPENSATING,
    CANCELLED,
    COMPENSATION_FAILED,
    TIMED_OUT,
}
