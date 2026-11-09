package devcoop.occount.payment.application.query.paymentlog

import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.PaymentType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.PageRequest
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

    @Test
    fun `paged getPaymentHistory returns paging meta`() {
        val logs = (1..25).map {
            PaymentLog(userId = 1L, paymentType = PaymentType.POINT, totalAmount = it * 100)
        }
        val service = GetPaymentHistoryQueryService(StubPaymentLogRepository(logs))

        val response = service.getPaymentHistory(1L, PageRequest.of(0, 10))

        assertEquals(10, response.payments.size)
        assertEquals(25L, response.totalCount)
        assertEquals(3, response.totalPages)
        assertEquals(0, response.currentPage)
        assertEquals(10, response.pageSize)
    }

    @Test
    fun `paged getPaymentHistoryByDateRange uses range query`() {
        val rangeLogs = (1..3).map {
            PaymentLog(userId = 1L, paymentType = PaymentType.CARD, totalAmount = it * 100)
        }
        val stub = StubPaymentLogRepository(emptyList(), rangeLogs = rangeLogs)
        val service = GetPaymentHistoryQueryService(stub)

        val response = service.getPaymentHistoryByDateRange(
            userId = 1L,
            startDate = LocalDateTime.of(2026, 6, 1, 0, 0),
            endDate = LocalDateTime.of(2026, 6, 30, 23, 59),
            pageable = PageRequest.of(0, 10),
        )

        assertEquals(3, response.payments.size)
        assertEquals(3L, response.totalCount)
    }

    private class StubPaymentLogRepository(
        private val logs: List<PaymentLog>,
        private val rangeLogs: List<PaymentLog> = logs,
    ) : PaymentLogRepository {
        override fun findById(paymentId: Long): PaymentLog? = logs.firstOrNull { it.getPaymentId() == paymentId }
        override fun findByUserId(userId: Long): List<PaymentLog> = logs
        override fun findByUserId(userId: Long, pageable: Pageable): Page<PaymentLog> = page(logs, pageable)
        override fun findByUserIdAndPaymentDateBetween(userId: Long, startDate: LocalDateTime, endDate: LocalDateTime): List<PaymentLog> = rangeLogs
        override fun findByUserIdAndPaymentDateBetween(
            userId: Long,
            startDate: LocalDateTime,
            endDate: LocalDateTime,
            pageable: Pageable,
        ): Page<PaymentLog> = page(rangeLogs, pageable)
        override fun findByPaymentType(paymentType: PaymentType): List<PaymentLog> = logs
        override fun save(paymentLog: PaymentLog): PaymentLog = paymentLog
        override fun saveAll(paymentLogs: List<PaymentLog>): List<PaymentLog> = paymentLogs

        private fun page(source: List<PaymentLog>, pageable: Pageable): Page<PaymentLog> {
            val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(source.size)
            val to = (from + pageable.pageSize).coerceAtMost(source.size)
            return PageImpl(source.subList(from, to), pageable, source.size.toLong())
        }
    }
}
