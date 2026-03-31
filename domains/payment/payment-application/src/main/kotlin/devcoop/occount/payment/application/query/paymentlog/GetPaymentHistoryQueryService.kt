package devcoop.occount.payment.application.query.paymentlog

import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.domain.payment.PaymentType
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class GetPaymentHistoryQueryService(
    private val paymentLogRepository: PaymentLogRepository,
) {
    fun getPaymentHistory(userId: Long): List<PaymentLogResult> {
        return paymentLogRepository.findByUserId(userId)
            .map(PaymentLogResult::from)
    }

    fun getPaymentHistoryByDateRange(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<PaymentLogResult> {
        return paymentLogRepository.findByUserIdAndPaymentDateBetween(userId, startDate, endDate)
            .map(PaymentLogResult::from)
    }

    fun getPaymentByType(paymentType: PaymentType): List<PaymentLogResult> {
        return paymentLogRepository.findByPaymentType(paymentType)
            .map(PaymentLogResult::from)
    }
}
