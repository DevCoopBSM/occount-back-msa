package devcoop.occount.payment.application.query.chargelog

import devcoop.occount.payment.application.output.ChargeLogRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminChargeLogQueryService(
    private val chargeLogRepository: ChargeLogRepository,
) {
    @Transactional(readOnly = true)
    fun findAll(pageable: Pageable): AdminChargeLogListResponse {
        val page = chargeLogRepository.findAll(pageable)
        return AdminChargeLogListResponse(
            charges = page.content.map(AdminChargeLogResult::from),
            totalCount = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            pageSize = page.size,
        )
    }
}
