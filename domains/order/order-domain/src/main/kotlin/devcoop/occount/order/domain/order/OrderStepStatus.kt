package devcoop.occount.order.domain.order

enum class OrderStepStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    COMPENSATED,
    COMPENSATION_FAILED,
}
