package devcoop.occount.investment.application.query

import devcoop.occount.investment.application.output.InvestmentRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetMyInvestmentsQueryService(
    private val investmentRepository: InvestmentRepository,
) {
    @Transactional(readOnly = true)
    fun getMyInvestments(userId: Long, pageable: Pageable): InvestmentListResponse {
        val page = investmentRepository.findByUserId(userId, pageable)
            .map(InvestmentResult::from)
        return InvestmentListResponse.from(page)
    }
}
