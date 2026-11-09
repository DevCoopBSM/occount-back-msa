package devcoop.occount.payment.application.output

import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.PaymentType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface PaymentLogRepository {
    fun findById(paymentId: Long): PaymentLog?
    fun findByUserId(userId: Long): List<PaymentLog>
    fun findByUserId(userId: Long, pageable: Pageable): Page<PaymentLog>
    fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentLog>
    fun findByUserIdAndPaymentDateBetween(
        userId: Long,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        pageable: Pageable,
    ): Page<PaymentLog>
    fun findByPaymentType(paymentType: PaymentType): List<PaymentLog>
    fun save(paymentLog: PaymentLog): PaymentLog
    fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog>
}
