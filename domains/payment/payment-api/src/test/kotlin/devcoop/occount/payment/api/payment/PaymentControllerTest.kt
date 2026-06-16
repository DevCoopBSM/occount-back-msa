package devcoop.occount.payment.api.payment

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.web.auth.AuthPrincipalArgumentResolver
import devcoop.occount.payment.api.support.ApiAdviceHandler
import devcoop.occount.payment.application.output.PaymentLogRepository
import devcoop.occount.payment.application.query.paymentlog.GetPaymentHistoryQueryService
import devcoop.occount.payment.domain.payment.PaymentLog
import devcoop.occount.payment.domain.payment.PaymentType
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.LocalDateTime
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.module.kotlin.jacksonMapperBuilder

class PaymentControllerTest {
    private fun mockMvc(repository: PaymentLogRepository): MockMvc {
        val objectMapper = jacksonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build()
        val controller = PaymentController(GetPaymentHistoryQueryService(repository))
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(ApiAdviceHandler())
            .setCustomArgumentResolvers(
                AuthPrincipalArgumentResolver(),
                PageableHandlerMethodArgumentResolver(),
            )
            .setMessageConverters(JacksonJsonHttpMessageConverter(objectMapper))
            .build()
    }

    private fun pointLog(amount: Int): PaymentLog =
        PaymentLog(userId = 1L, paymentType = PaymentType.POINT, totalAmount = amount)

    @Test
    fun `getPaymentHistory returns 200 with snake_case paging meta`() {
        val logs = (1..25).map { pointLog(it * 100) }
        val mockMvc = mockMvc(FakePaymentLogRepository(logs))

        mockMvc.perform(
            get("/payments/history")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1")
                .param("page", "0")
                .param("size", "10"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.payments.length()").value(10))
            .andExpect(jsonPath("$.total_count").value(25))
            .andExpect(jsonPath("$.total_pages").value(3))
            .andExpect(jsonPath("$.current_page").value(0))
            .andExpect(jsonPath("$.page_size").value(10))
            .andExpect(jsonPath("$.payments[0].payment_type").value("POINT"))
            .andExpect(jsonPath("$.payments[0].total_amount").exists())
    }

    @Test
    fun `getPaymentHistory caps page size at 100`() {
        val mockMvc = mockMvc(FakePaymentLogRepository((1..5).map { pointLog(it * 100) }))

        mockMvc.perform(
            get("/payments/history")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1")
                .param("size", "500"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.page_size").value(100))
    }

    @Test
    fun `getPaymentHistory with date range uses range query`() {
        val all = (1..5).map { pointLog(it * 100) }
        val ranged = (1..2).map { pointLog(it * 100) }
        val mockMvc = mockMvc(FakePaymentLogRepository(all, rangeLogs = ranged))

        mockMvc.perform(
            get("/payments/history")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1")
                .param("start_date", "2026-06-01T00:00:00")
                .param("end_date", "2026-06-30T23:59:59"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total_count").value(2))
    }

    @Test
    fun `getPaymentHistory without auth header returns error`() {
        val mockMvc = mockMvc(FakePaymentLogRepository(emptyList()))

        mockMvc.perform(get("/payments/history"))
            .andExpect(status().is4xxClientError)
    }

    private class FakePaymentLogRepository(
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
