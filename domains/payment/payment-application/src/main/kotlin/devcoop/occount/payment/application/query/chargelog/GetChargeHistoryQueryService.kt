package devcoop.occount.payment.application.query.chargelog

import devcoop.occount.payment.application.output.ChargeLogRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GetChargeHistoryQueryService(
    private val chargeLogRepository: ChargeLogRepository,
) {
    @Transactional(readOnly = true)
    fun getChargeHistory(userId: Long, pageable: Pageable): ChargeHistoryListResponse {
        val page = chargeLogRepository.findByUserId(userId, pageable)
            .map(ChargeLogResult::from)
        return ChargeHistoryListResponse.from(page)
    }
}
