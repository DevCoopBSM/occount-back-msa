package devcoop.occount.payment.application.output

import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.PaymentType
import java.time.LocalDateTime

interface PaymentLogRepository {
    fun findByUserId(userId: Long): List<PaymentLog>
    fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentLog>
    fun findByPaymentType(paymentType: PaymentType): List<PaymentLog>
    fun save(paymentLog: PaymentLog): PaymentLog
    fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog>
}
