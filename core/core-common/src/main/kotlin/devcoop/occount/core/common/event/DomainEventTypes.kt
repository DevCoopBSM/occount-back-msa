package devcoop.occount.core.common.event

object DomainEventTypes {
    const val MEMBER_REGISTERED = "MemberRegisteredEvent"
    const val ORDER_REQUESTED = "OrderRequestedEvent"
    const val ORDER_PAYMENT_REQUESTED = "OrderPaymentRequestedEvent"
    const val ORDER_PAYMENT_CANCELLATION_REQUESTED = "OrderPaymentCancellationRequestedEvent"
    const val ITEM_STOCK_DECREASED = "ItemStockDecreasedEvent"
    const val ITEM_STOCK_DECREASE_FAILED = "ItemStockDecreaseFailedEvent"
    const val PAYMENT_COMPLETED = "PaymentCompletedEvent"
    const val PAYMENT_FAILED = "PaymentFailedEvent"
    const val ORDER_PAYMENT_COMPENSATION_REQUESTED = "OrderPaymentCompensationRequestedEvent"
    const val PAYMENT_COMPENSATION_FAILED = "PaymentCompensationFailedEvent"
    const val PAYMENT_COMPENSATED = "PaymentCompensatedEvent"
    const val ITEM_STOCK_COMPENSATION_REQUESTED = "ItemStockCompensationRequestedEvent"
    const val ITEM_STOCK_COMPENSATED = "ItemStockCompensatedEvent"
    const val ITEM_STOCK_COMPENSATION_FAILED = "ItemStockCompensationFailedEvent"
    const val POINT_BALANCE_CHANGED = "PointBalanceChangedEvent"
    const val POINT_INITIALIZED = "PointInitializedEvent"
}
