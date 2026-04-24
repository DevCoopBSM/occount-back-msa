package devcoop.occount.core.common.event

object DomainTopics {
    const val MEMBER_REGISTERED = "member.registered.v1"
    const val ORDER_REQUESTED = "order.requested.v1"
    const val ORDER_PAYMENT_REQUESTED = "order.payment-requested.v1"
    const val ORDER_PAYMENT_CANCELLATION_REQUESTED = "order.payment-cancellation-requested.v1"
    const val PAYMENT_COMPLETED = "payment.completed.v1"
    const val PAYMENT_FAILED = "payment.failed.v1"
    const val ORDER_PAYMENT_COMPENSATION_REQUESTED = "order.payment-compensation-requested.v1"
    const val PAYMENT_COMPENSATION_FAILED = "payment.compensation-failed.v1"
    const val PAYMENT_COMPENSATED = "payment.compensated.v1"
    const val ITEM_STOCK_DECREASED = "item.stock-decreased.v1"
    const val ITEM_STOCK_DECREASE_FAILED = "item.stock-decrease-failed.v1"
    const val ITEM_STOCK_COMPENSATED = "item.stock-compensated.v1"
    const val ITEM_STOCK_COMPENSATION_FAILED = "item.stock-compensation-failed.v1"
    const val ITEM_STOCK_COMPENSATION_REQUESTED = "item.stock-compensation-requested.v1"
    const val POINT_BALANCE_CHANGED = "point.balance-changed.v1"
    const val POINT_INITIALIZED = "point.initialized.v1"
}
