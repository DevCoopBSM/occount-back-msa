package devcoop.occount.payment.application.query.chargelog

import devcoop.occount.payment.application.output.ChargeLogRepository
import org.springframework.stereotype.Service

@Service
class GetChargeHistoryQueryService(
    private val chargeLogRepository: ChargeLogRepository,
) {
    fun getChargeHistory(userId: Long): List<ChargeLogResult> {
        return chargeLogRepository.findByUserId(userId)
            .map(ChargeLogResult::from)
    }
}
