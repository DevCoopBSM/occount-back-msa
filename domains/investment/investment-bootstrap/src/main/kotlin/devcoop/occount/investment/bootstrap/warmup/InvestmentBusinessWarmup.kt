package devcoop.occount.investment.bootstrap.warmup

import devcoop.occount.investment.application.output.InvestmentRepository
import devcoop.occount.warmup.BusinessWarmup
import devcoop.occount.warmup.WarmupProbe
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component

@Component
class InvestmentBusinessWarmup(
    private val investmentRepository: InvestmentRepository,
) : BusinessWarmup {
    override fun warmup() {
        investmentRepository.findByUserId(WarmupProbe.USER_ID, PageRequest.of(0, 1))
    }
}
