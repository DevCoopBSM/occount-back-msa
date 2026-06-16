package devcoop.occount.investment.application.query

import devcoop.occount.investment.domain.investment.Investment
import devcoop.occount.investment.domain.investment.InvestmentStatus
import devcoop.occount.investment.domain.investment.InvestmentType
import java.time.LocalDateTime

/**
 * 관리자 출자 조회 결과. 운영용으로 userId, paymentId 까지 노출한다.
 */
data class AdminInvestmentResult(
    val investmentId: Long,
    val userId: Long,
    val paymentId: String,
    val amount: Int,
    val type: InvestmentType,
    val status: InvestmentStatus,
    val depositDate: LocalDateTime?,
    val conversionDate: LocalDateTime?,
    val confirmMethod: String,
) {
    companion object {
        fun from(investment: Investment): AdminInvestmentResult {
            return AdminInvestmentResult(
                investmentId = investment.investmentId,
                userId = investment.userId,
                paymentId = investment.paymentId,
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
