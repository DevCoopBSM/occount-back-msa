package devcoop.occount.investment.application.query

import devcoop.occount.investment.application.exception.InvestmentAccessDeniedException
import devcoop.occount.investment.application.exception.InvestmentNotFoundException
import devcoop.occount.investment.application.output.InvestmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetInvestmentDetailQueryService(
    private val investmentRepository: InvestmentRepository,
) {
    /**
     * 본인 출자 상세 조회. 다른 사용자의 출자에 접근하면 거부한다.
     */
    @Transactional(readOnly = true)
    fun getDetail(investmentId: Long, requesterUserId: Long): InvestmentResult {
        val investment = investmentRepository.findById(investmentId)
            ?: throw InvestmentNotFoundException()
        if (investment.userId != requesterUserId) {
            throw InvestmentAccessDeniedException()
        }
        return InvestmentResult.from(investment)
    }
}
