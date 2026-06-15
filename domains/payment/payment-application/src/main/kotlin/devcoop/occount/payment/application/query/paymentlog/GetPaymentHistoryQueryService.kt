package devcoop.occount.payment.application.query.paymentlog

import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.domain.payment.PaymentType
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GetPaymentHistoryQueryService(
    private val paymentLogRepository: PaymentLogRepository,
) {
    fun getPaymentHistory(userId: Long): List<PaymentLogResult> {
        return paymentLogRepository.findByUserId(userId)
            .map(PaymentLogResult::from)
    }

    @Transactional(readOnly = true)
    fun getPaymentHistory(userId: Long, pageable: Pageable): PaymentHistoryListResponse {
        val page = paymentLogRepository.findByUserId(userId, pageable)
            .map(PaymentLogResult::from)
        return PaymentHistoryListResponse.from(page)
    }

    fun getPaymentHistoryByDateRange(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<PaymentLogResult> {
        return paymentLogRepository.findByUserIdAndPaymentDateBetween(userId, startDate, endDate)
            .map(PaymentLogResult::from)
    }

    @Transactional(readOnly = true)
    fun getPaymentHistoryByDateRange(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable,
    ): PaymentHistoryListResponse {
        val page = paymentLogRepository
            .findByUserIdAndPaymentDateBetween(userId, startDate, endDate, pageable)
            .map(PaymentLogResult::from)
        return PaymentHistoryListResponse.from(page)
    }

    fun getPaymentByType(paymentType: PaymentType): List<PaymentLogResult> {
        return paymentLogRepository.findByPaymentType(paymentType)
            .map(PaymentLogResult::from)
    }
}
