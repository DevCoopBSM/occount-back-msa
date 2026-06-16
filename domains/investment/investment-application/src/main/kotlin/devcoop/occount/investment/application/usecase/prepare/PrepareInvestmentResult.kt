package devcoop.occount.investment.application.usecase.prepare

/**
 * 출자 결제 시작 결과. 프론트는 발급된 paymentId 로 PortOne 결제창을 연다.
 */
data class PrepareInvestmentResult(
    val paymentId: String,
)
