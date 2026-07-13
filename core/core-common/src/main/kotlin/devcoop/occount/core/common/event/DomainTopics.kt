package devcoop.occount.core.common.event

object DomainTopics {
    const val MEMBER_REGISTERED = "member.event.registered.v1"
    const val ORDER_REQUESTED = "order.event.requested.v1"
    const val PAYMENT_COMMANDS = "payment.command.v1"
    // 결제 취소 커맨드 전용 토픽. 결제 커맨드와 분리해 별도 리스너/스레드에서 처리한다 →
    // 진행 중 VAN 결제가 컨슈머 스레드를 블로킹해도 취소가 즉시 실행돼 단말을 중단시킬 수 있다.
    const val PAYMENT_CANCEL_COMMANDS = "payment.command.cancel.v1"
    const val PAYMENT_EVENTS = "payment.event.v1"
    const val ITEM_EVENTS = "item.event.stock.v1"
    const val ITEM_STOCK_COMPENSATION_REQUESTED = "item.command.compensation.v1"
}
