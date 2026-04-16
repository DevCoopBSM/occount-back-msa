package devcoop.occount.core.common.event

object DomainEventTypes {
    const val MEMBER_REGISTERED = "MemberRegisteredEvent"
    const val ORDER_REQUESTED = "OrderRequestedEvent"
    const val ORDER_PAYMENT_REQUESTED = "OrderPaymentRequestedEvent"
    const val ORDER_PAYMENT_COMPENSATION_REQUESTED = "OrderPaymentCompensationRequestedEvent"
    const val ORDER_STOCK_COMPENSATION_REQUESTED = "OrderStockCompensationRequestedEvent"
    const val POINT_BALANCE_CHANGED = "PointBalanceChangedEvent"
    const val POINT_INITIALIZED = "PointInitializedEvent"
}
