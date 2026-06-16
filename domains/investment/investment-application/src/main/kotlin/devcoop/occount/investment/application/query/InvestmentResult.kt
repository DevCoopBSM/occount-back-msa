package devcoop.occount.investment.application.query

import devcoop.occount.investment.domain.investment.Investment
import devcoop.occount.investment.domain.investment.InvestmentStatus
import devcoop.occount.investment.domain.investment.InvestmentType
import java.time.LocalDateTime

/**
 * 사용자 본인 출자 조회 결과. 내부 식별자(userId, paymentId)는 노출하지 않는다.
 */
data class InvestmentResult(
    val investmentId: Long,
    val amount: Int,
    val type: InvestmentType,
    val status: InvestmentStatus,
    val depositDate: LocalDateTime?,
    val conversionDate: LocalDateTime?,
    val confirmMethod: String,
) {
    companion object {
        fun from(investment: Investment): InvestmentResult {
            return InvestmentResult(
                investmentId = investment.investmentId,
                amount = investment.amount,
                type = investment.type,
                status = investment.status,
                depositDate = investment.depositDate,
                conversionDate = investment.conversionDate,
                confirmMethod = investment.confirmMethod,
            )
        }
    }
}
