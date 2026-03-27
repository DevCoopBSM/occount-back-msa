package devcoop.occount.payment.domain

import devcoop.occount.payment.domain.type.PaymentType
import java.time.LocalDateTime

interface PaymentLogRepository {
    fun findByUserId(userId: Long): List<PaymentLog>
    fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentLog>
    fun findByPaymentType(paymentType: PaymentType): List<PaymentLog>
    fun save(paymentLog: PaymentLog): PaymentLog
    fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog>
}
