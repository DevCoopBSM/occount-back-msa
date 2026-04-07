package devcoop.occount.core.common.event

object DomainEventTypes {
    const val MEMBER_REGISTERED = "MemberRegisteredEvent"
    const val ORDER_REQUESTED = "OrderRequestedEvent"
    const val ORDER_PAYMENT_COMPLETED = "OrderPaymentCompletedEvent"
    const val ORDER_PAYMENT_FAILED = "OrderPaymentFailedEvent"
    const val ORDER_STOCK_COMPLETED = "OrderStockCompletedEvent"
    const val ORDER_STOCK_FAILED = "OrderStockFailedEvent"
    const val ORDER_PAYMENT_COMPENSATION_REQUESTED = "OrderPaymentCompensationRequestedEvent"
    const val ORDER_PAYMENT_COMPENSATED = "OrderPaymentCompensatedEvent"
    const val ORDER_PAYMENT_COMPENSATION_FAILED = "OrderPaymentCompensationFailedEvent"
    const val ORDER_STOCK_COMPENSATION_REQUESTED = "OrderStockCompensationRequestedEvent"
    const val ORDER_STOCK_COMPENSATED = "OrderStockCompensatedEvent"
    const val ORDER_STOCK_COMPENSATION_FAILED = "OrderStockCompensationFailedEvent"
    const val POINT_BALANCE_CHANGED = "PointBalanceChangedEvent"
    const val POINT_INITIALIZED = "PointInitializedEvent"
}
