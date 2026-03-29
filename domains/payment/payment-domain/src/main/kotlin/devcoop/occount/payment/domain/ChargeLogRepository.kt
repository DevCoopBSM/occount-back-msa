package devcoop.occount.payment.domain

import devcoop.occount.payment.domain.type.RefundState
import java.time.LocalDateTime

interface ChargeLogRepository {
    fun findByUserId(userId: Long): List<ChargeLog>
    fun findByPaymentId(paymentId: String): ChargeLog?
    fun findByRefundState(refundState: RefundState): List<ChargeLog>
    fun findByUserIdAndChargeDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<ChargeLog>
    fun save(chargeLog: ChargeLog): ChargeLog
    fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog>
}
