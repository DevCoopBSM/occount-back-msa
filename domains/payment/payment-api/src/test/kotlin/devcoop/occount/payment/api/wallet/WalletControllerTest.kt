package devcoop.occount.payment.api.wallet

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.web.auth.AuthPrincipalArgumentResolver
import devcoop.occount.payment.api.support.ApiAdviceHandler
import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.application.query.chargelog.GetChargeHistoryQueryService
import devcoop.occount.payment.application.query.wallet.GetWalletPointQueryService
import devcoop.occount.payment.domain.wallet.ChargeLog
import devcoop.occount.payment.domain.wallet.ChargeReason
import devcoop.occount.payment.domain.wallet.PointTransaction
import devcoop.occount.payment.domain.wallet.Wallet
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

class WalletControllerTest {
    private fun mockMvc(logs: List<ChargeLog>): MockMvc {
        val objectMapper = jacksonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build()
        val controller = WalletController(
            GetWalletPointQueryService(NoopWalletRepository()),
            GetChargeHistoryQueryService(FakeChargeLogRepository(logs)),
        )
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(ApiAdviceHandler())
            .setCustomArgumentResolvers(
                AuthPrincipalArgumentResolver(),
                PageableHandlerMethodArgumentResolver(),
            )
            .setMessageConverters(JacksonJsonHttpMessageConverter(objectMapper))
            .build()
    }

    private fun chargeLog(chargeId: Long, userId: Long = 1L, chargedBy: Long? = 99L): ChargeLog = ChargeLog(
        chargeId = chargeId,
        userId = userId,
        chargeDate = LocalDateTime.of(2026, 6, 11, 9, 0),
        pointTransaction = PointTransaction(beforePoint = 0, changeAmount = 1000, afterPoint = 1000),
        chargeReason = ChargeReason.ADMIN,
        detailReason = "보상",
        chargedBy = chargedBy,
    )

    @Test
    fun `getChargeHistory returns 200 with snake_case paging meta`() {
        val logs = (1L..25L).map { chargeLog(chargeId = it, userId = 1L) }
        val mockMvc = mockMvc(logs)

        mockMvc.perform(
            get("/wallet/charges")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1")
                .param("page", "0")
                .param("size", "10"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.charges.length()").value(10))
            .andExpect(jsonPath("$.total_count").value(25))
            .andExpect(jsonPath("$.total_pages").value(3))
            .andExpect(jsonPath("$.current_page").value(0))
            .andExpect(jsonPath("$.page_size").value(10))
            .andExpect(jsonPath("$.charges[0].charge_id").exists())
            .andExpect(jsonPath("$.charges[0].charge_date").exists())
            .andExpect(jsonPath("$.charges[0].before_point").value(0))
            .andExpect(jsonPath("$.charges[0].change_amount").value(1000))
            .andExpect(jsonPath("$.charges[0].after_point").value(1000))
    }

    @Test
    fun `getChargeHistory returns only own charges`() {
        val logs = listOf(
            chargeLog(chargeId = 1L, userId = 1L),
            chargeLog(chargeId = 2L, userId = 2L),
            chargeLog(chargeId = 3L, userId = 1L),
        )
        val mockMvc = mockMvc(logs)

        mockMvc.perform(
            get("/wallet/charges").header(AuthHeaders.AUTHENTICATED_USER_ID, "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total_count").value(2))
            .andExpect(jsonPath("$.charges[0].charge_id").value(3))
            .andExpect(jsonPath("$.charges[1].charge_id").value(1))
    }

    @Test
    fun `getChargeHistory does not expose charged_by audit field`() {
        val mockMvc = mockMvc(listOf(chargeLog(chargeId = 1L, userId = 1L, chargedBy = 99L)))

        mockMvc.perform(
            get("/wallet/charges").header(AuthHeaders.AUTHENTICATED_USER_ID, "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.charges[0].charged_by").doesNotExist())
            .andExpect(jsonPath("$.charges[0].user_id").doesNotExist())
    }

    @Test
    fun `getChargeHistory caps page size at 100`() {
        val logs = (1L..5L).map { chargeLog(chargeId = it, userId = 1L) }
        val mockMvc = mockMvc(logs)

        mockMvc.perform(
            get("/wallet/charges")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1")
                .param("size", "500"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.page_size").value(100))
    }

    @Test
    fun `getChargeHistory sorts by chargeDate when requested`() {
        val logs = listOf(
            chargeLog(chargeId = 1L, userId = 1L).copy(chargeDate = LocalDateTime.of(2026, 6, 13, 9, 0)),
            chargeLog(chargeId = 2L, userId = 1L).copy(chargeDate = LocalDateTime.of(2026, 6, 11, 9, 0)),
            chargeLog(chargeId = 3L, userId = 1L).copy(chargeDate = LocalDateTime.of(2026, 6, 12, 9, 0)),
        )
        val mockMvc = mockMvc(logs)

        mockMvc.perform(
            get("/wallet/charges")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1")
                .param("sort", "chargeDate,asc"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.charges[0].charge_id").value(2))
            .andExpect(jsonPath("$.charges[1].charge_id").value(3))
            .andExpect(jsonPath("$.charges[2].charge_id").value(1))
    }

    @Test
    fun `getChargeHistory without auth header returns 401`() {
        val mockMvc = mockMvc(emptyList())

        mockMvc.perform(get("/wallet/charges"))
            .andExpect(status().isUnauthorized)
    }

    private class FakeChargeLogRepository(initial: List<ChargeLog>) : ChargeLogRepository {
        private val logs = initial.toList()
        override fun findByPaymentId(paymentId: Long): ChargeLog? = logs.firstOrNull { it.paymentId == paymentId }
        override fun findAll(pageable: Pageable): Page<ChargeLog> = page(logs, pageable)
        override fun findByUserId(userId: Long, pageable: Pageable): Page<ChargeLog> =
            page(logs.filter { it.userId == userId }, pageable)
        override fun save(chargeLog: ChargeLog): ChargeLog = chargeLog
        override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> = chargeLogs

        private fun page(source: List<ChargeLog>, pageable: Pageable): Page<ChargeLog> {
            val sorted = applySort(source, pageable.sort)
            val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(sorted.size)
            val to = (from + pageable.pageSize).coerceAtMost(sorted.size)
            return PageImpl(sorted.subList(from, to), pageable, sorted.size.toLong())
        }

        private fun applySort(source: List<ChargeLog>, sort: org.springframework.data.domain.Sort): List<ChargeLog> {
            val order = sort.firstOrNull() ?: return source.sortedByDescending { it.chargeId }
            val comparator: Comparator<ChargeLog> = when (order.property) {
                "chargeDate" -> compareBy { it.chargeDate }
                else -> compareBy { it.chargeId }
            }
            return source.sortedWith(if (order.isAscending) comparator else comparator.reversed())
        }
    }

    private class NoopWalletRepository : WalletRepository {
        override fun findByUserId(userId: Long): Wallet? = null
        override fun save(wallet: Wallet): Wallet = wallet
    }
}
