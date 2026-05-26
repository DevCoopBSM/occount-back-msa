package devcoop.occount.core.common.event

object DomainTopics {
    const val MEMBER_REGISTERED = "member.event.registered.v1"
    const val ORDER_REQUESTED = "order.event.requested.v1"
    const val PAYMENT_COMMANDS = "payment.command.v1"
    const val PAYMENT_EVENTS = "payment.event.v1"
    const val ITEM_EVENTS = "item.event.stock.v1"
    const val ITEM_STOCK_COMPENSATION_REQUESTED = "item.command.compensation.v1"
}
