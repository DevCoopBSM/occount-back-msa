package devcoop.occount.core.common.event

object DomainTopics {
    const val MEMBER_REGISTERED = "member.registered.v1"
    const val ORDER_REQUESTED = "order.requested.v1"
    const val ORDER_PAYMENT_REQUESTED = "order.payment-requested.v1"
    const val ORDER_PAYMENT_COMPENSATION_REQUESTED = "order.payment-compensation-requested.v1"
    const val ORDER_STOCK_COMPENSATION_REQUESTED = "order.stock-compensation-requested.v1"
    const val POINT_BALANCE_CHANGED = "point.balance-changed.v1"
    const val POINT_INITIALIZED = "point.initialized.v1"
}
