package devcoop.occount.payment.application.query.chargelog

import devcoop.occount.payment.domain.ChargeLogRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GetChargeHistoryQueryService(
    private val chargeLogRepository: ChargeLogRepository,
) {
    fun getChargeHistory(userId: Long): List<ChargeLogResult> {
        return chargeLogRepository.findByUserId(userId)
            .map(ChargeLogResult::from)
    }

    fun getChargeHistoryByDateRange(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<ChargeLogResult> {
        return chargeLogRepository.findByUserIdAndChargeDateBetween(userId, startDate, endDate)
            .map(ChargeLogResult::from)
    }
}
