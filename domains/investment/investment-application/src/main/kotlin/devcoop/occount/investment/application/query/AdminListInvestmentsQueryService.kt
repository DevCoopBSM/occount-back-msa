package devcoop.occount.investment.application.query

import devcoop.occount.investment.application.output.InvestmentRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminListInvestmentsQueryService(
    private val investmentRepository: InvestmentRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): AdminInvestmentListResponse {
        val page = investmentRepository.findAll(pageable)
            .map(AdminInvestmentResult::from)
        return AdminInvestmentListResponse.from(page)
    }
}
