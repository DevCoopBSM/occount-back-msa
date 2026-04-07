package devcoop.occount.core.common.event

object DomainTopics {
    const val MEMBER_REGISTERED = "member.registered.v1"
    const val ORDER_REQUESTED = "order.requested.v1"
    const val ORDER_PAYMENT_COMPLETED = "order.payment-completed.v1"
    const val ORDER_PAYMENT_FAILED = "order.payment-failed.v1"
    const val ORDER_STOCK_COMPLETED = "order.stock-completed.v1"
    const val ORDER_STOCK_FAILED = "order.stock-failed.v1"
    const val ORDER_PAYMENT_COMPENSATION_REQUESTED = "order.payment-compensation-requested.v1"
    const val ORDER_PAYMENT_COMPENSATED = "order.payment-compensated.v1"
    const val ORDER_PAYMENT_COMPENSATION_FAILED = "order.payment-compensation-failed.v1"
    const val ORDER_STOCK_COMPENSATION_REQUESTED = "order.stock-compensation-requested.v1"
    const val ORDER_STOCK_COMPENSATED = "order.stock-compensated.v1"
    const val ORDER_STOCK_COMPENSATION_FAILED = "order.stock-compensation-failed.v1"
    const val POINT_BALANCE_CHANGED = "point.balance-changed.v1"
    const val POINT_INITIALIZED = "point.initialized.v1"
}
