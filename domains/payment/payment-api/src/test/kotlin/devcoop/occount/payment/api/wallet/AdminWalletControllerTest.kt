package devcoop.occount.payment.api.wallet

import devcoop.occount.payment.api.support.ApiAdviceHandler
import devcoop.occount.payment.application.output.ChargeLogRepository
import devcoop.occount.payment.application.output.WalletRepository
import devcoop.occount.payment.application.query.chargelog.AdminChargeLogQueryService
import devcoop.occount.payment.application.usecase.wallet.admincharge.AdminChargeWalletUseCase
import devcoop.occount.payment.application.usecase.wallet.admincharge.WalletChargeProcessor
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

class AdminWalletControllerTest {
    private fun mockMvc(controller: AdminWalletController): MockMvc {
        val objectMapper = jacksonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build()
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(ApiAdviceHandler())
            .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
            .setMessageConverters(JacksonJsonHttpMessageConverter(objectMapper))
            .build()
    }

    private fun controller(logs: List<ChargeLog>): AdminWalletController {
        val chargeLogRepository = FakeChargeLogRepository(logs)
        val useCase = AdminChargeWalletUseCase(
            WalletChargeProcessor(NoopWalletRepository(), chargeLogRepository),
        )
        return AdminWalletController(useCase, AdminChargeLogQueryService(chargeLogRepository))
    }

    private fun chargeLog(chargeId: Long, chargedBy: Long? = 99L): ChargeLog = ChargeLog(
        chargeId = chargeId,
        userId = 1L,
        chargeDate = LocalDateTime.of(2026, 6, 11, 9, 0),
        pointTransaction = PointTransaction(beforePoint = 0, changeAmount = 1000, afterPoint = 1000),
        chargeReason = ChargeReason.ADMIN,
        detailReason = "보상",
        chargedBy = chargedBy,
    )

    @Test
    fun `findChargeHistory returns 200 with snake_case paging meta`() {
        val logs = (1L..25L).map { chargeLog(chargeId = it) }
        val mockMvc = mockMvc(controller(logs))

        mockMvc.perform(get("/wallet/admin/charges").param("page", "0").param("size", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.charges.length()").value(10))
            .andExpect(jsonPath("$.total_count").value(25))
            .andExpect(jsonPath("$.total_pages").value(3))
            .andExpect(jsonPath("$.current_page").value(0))
            .andExpect(jsonPath("$.page_size").value(10))
            .andExpect(jsonPath("$.charges[0].charge_id").exists())
            .andExpect(jsonPath("$.charges[0].charged_by").value(99))
            .andExpect(jsonPath("$.charges[0].charge_date").exists())
            .andExpect(jsonPath("$.charges[0].before_point").value(0))
            .andExpect(jsonPath("$.charges[0].change_amount").value(1000))
            .andExpect(jsonPath("$.charges[0].after_point").value(1000))
    }

    @Test
    fun `findChargeHistory defaults to latest first and size 10`() {
        val logs = (1L..3L).map { chargeLog(chargeId = it) }
        val mockMvc = mockMvc(controller(logs))

        mockMvc.perform(get("/wallet/admin/charges"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page_size").value(10))
            .andExpect(jsonPath("$.charges[0].charge_id").value(3))
            .andExpect(jsonPath("$.charges[2].charge_id").value(1))
    }

    @Test
    fun `findChargeHistory caps page size at 100`() {
        val logs = (1L..5L).map { chargeLog(chargeId = it) }
        val mockMvc = mockMvc(controller(logs))

        mockMvc.perform(get("/wallet/admin/charges").param("size", "500"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.page_size").value(100))
    }

    @Test
    fun `findChargeHistory ignores non-whitelisted sort field`() {
        val logs = (1L..3L).map { chargeLog(chargeId = it) }
        val mockMvc = mockMvc(controller(logs))

        // 허용 외 정렬 필드 → 무시되고 기본 정렬(chargeId DESC) 적용
        mockMvc.perform(get("/wallet/admin/charges").param("sort", "detailReason,asc"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.charges[0].charge_id").value(3))
    }

    private class FakeChargeLogRepository(initial: List<ChargeLog>) : ChargeLogRepository {
        private val logs = initial.toList()
        override fun findByPaymentId(paymentId: Long): ChargeLog? = logs.firstOrNull { it.paymentId == paymentId }
        override fun findAll(pageable: Pageable): Page<ChargeLog> {
            val sorted = sortDescById(logs, pageable)
            val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(sorted.size)
            val to = (from + pageable.pageSize).coerceAtMost(sorted.size)
            return PageImpl(sorted.subList(from, to), pageable, sorted.size.toLong())
        }
        override fun findByUserId(userId: Long, pageable: Pageable): Page<ChargeLog> {
            val filtered = logs.filter { it.userId == userId }
            val sorted = sortDescById(filtered, pageable)
            val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(sorted.size)
            val to = (from + pageable.pageSize).coerceAtMost(sorted.size)
            return PageImpl(sorted.subList(from, to), pageable, sorted.size.toLong())
        }
        override fun save(chargeLog: ChargeLog): ChargeLog = chargeLog
        override fun saveAll(chargeLogs: List<ChargeLog>): List<ChargeLog> = chargeLogs

        private fun sortDescById(source: List<ChargeLog>, pageable: Pageable): List<ChargeLog> {
            val order = pageable.sort.firstOrNull() ?: return source.sortedByDescending { it.chargeId }
            val comparator = compareBy<ChargeLog> { it.chargeId }
            return source.sortedWith(if (order.isAscending) comparator else comparator.reversed())
        }
    }

    private class NoopWalletRepository : WalletRepository {
        override fun findByUserId(userId: Long): Wallet? = null
        override fun save(wallet: Wallet): Wallet = wallet
    }
}
