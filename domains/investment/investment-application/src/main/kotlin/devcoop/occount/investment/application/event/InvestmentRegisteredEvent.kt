package devcoop.occount.investment.application.event

/**
 * 출자가 확정(등록)되었음을 알리는 도메인 이벤트.
 * member 도메인이 소비하여 첫 출자 시 회원 등급을 승격한다.
 */
data class InvestmentRegisteredEvent(
    val investmentId: Long,
    val userId: Long,
    val amount: Int,
)
