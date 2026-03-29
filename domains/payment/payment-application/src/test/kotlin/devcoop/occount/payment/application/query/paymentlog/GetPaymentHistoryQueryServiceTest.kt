package devcoop.occount.payment.application.query.paymentlog

import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.PaymentLogRepository
import devcoop.occount.payment.domain.type.PaymentType
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class GetPaymentHistoryQueryServiceTest {
    @Test
    fun `getPaymentHistory delegates to repository`() {
        val paymentLogs = listOf(
            PaymentLog(
                userId = 1L,
                paymentType = PaymentType.POINT,
                totalAmount = 1000,
            ),
        )
        val service = GetPaymentHistoryQueryService(StubPaymentLogRepository(paymentLogs))

        val result = service.getPaymentHistory(1L)

        assertEquals(1, result.size)
        assertEquals(paymentLogs.single().getUserId(), result.single().userId)
        assertEquals(paymentLogs.single().getTotalAmount(), result.single().totalAmount)
    }

    private class StubPaymentLogRepository(
        private val logs: List<PaymentLog>,
    ) : PaymentLogRepository {
        override fun findByUserId(userId: Long): List<PaymentLog> = logs
        override fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentLog> = logs
        override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> = logs
        override fun save(paymentLog: PaymentLog): PaymentLog = paymentLog
        override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> = paymentLogs
    }
}
