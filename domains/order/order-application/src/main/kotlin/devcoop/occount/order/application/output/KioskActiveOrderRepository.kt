package devcoop.occount.order.application.output

interface KioskActiveOrderRepository {
    /**
     * 키오스크의 활성 주문을 [newOrderId] 로 원자적으로 교체하고, 밀려난 이전 활성 주문 id 를 반환한다.
     *
     * 이전 활성 주문이 없으면(= 키오스크의 첫 주문) `null` 을 반환한다.
     * 같은 키오스크에서 동시에 들어온 주문(연타)은 비관적 락으로 직렬화되어, 마지막에 claim 한 주문만
     * 활성으로 남고 나머지는 각각 밀려난 주문으로 반환된다 → 호출측이 취소(supersede)한다.
     * VAN 단말은 키오스크당 1대이므로 활성 주문도 키오스크당 1건이어야 이중결제가 생기지 않는다.
     */
    fun claimActiveOrder(kioskId: String, newOrderId: Long): Long?
}
