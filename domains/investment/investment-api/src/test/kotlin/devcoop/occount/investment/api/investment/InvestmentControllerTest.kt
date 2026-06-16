package devcoop.occount.investment.api.investment

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.core.common.auth.AuthPrincipalArgumentResolver
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.investment.api.support.ApiAdviceHandler
import devcoop.occount.investment.application.output.InvestmentRepository
import devcoop.occount.investment.application.query.AdminListInvestmentsQueryService
import devcoop.occount.investment.application.query.GetInvestmentDetailQueryService
import devcoop.occount.investment.application.query.GetMyInvestmentsQueryService
import devcoop.occount.investment.application.usecase.admin.CreateInvestmentByAdminUseCase
import devcoop.occount.investment.application.usecase.prepare.PrepareInvestmentUseCase
import devcoop.occount.investment.domain.investment.Investment
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.module.kotlin.jacksonMapperBuilder

class InvestmentControllerTest {
    private val repository = FakeInvestmentRepository()

    private fun mockMvc(): MockMvc {
        val objectMapper = jacksonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build()
        val controller = InvestmentController(
            prepareInvestmentUseCase = PrepareInvestmentUseCase(repository),
            createInvestmentByAdminUseCase = CreateInvestmentByAdminUseCase(repository, NoopEventPublisher),
            getMyInvestmentsQueryService = GetMyInvestmentsQueryService(repository),
            getInvestmentDetailQueryService = GetInvestmentDetailQueryService(repository),
            adminListInvestmentsQueryService = AdminListInvestmentsQueryService(repository),
        )
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(ApiAdviceHandler())
            .setCustomArgumentResolvers(
                AuthPrincipalArgumentResolver(),
                PageableHandlerMethodArgumentResolver(),
            )
            .setMessageConverters(StringHttpMessageConverter(), JacksonJsonHttpMessageConverter(objectMapper))
            .build()
    }

    @Test
    fun `create returns 201 with snake_case payment_id`() {
        mockMvc().perform(
            post("/investments")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1")
                .contentType("application/json")
                .content("""{"amount": 50000}"""),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.payment_id").exists())

        val saved = repository.saved.single()
        assert(saved.userId == 1L)
        assert(saved.amount == 50000)
    }

    @Test
    fun `create rejects non-positive amount with 400`() {
        mockMvc().perform(
            post("/investments")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "1")
                .contentType("application/json")
                .content("""{"amount": 0}"""),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `getMyInvestments returns only own with snake_case paging meta and hides internal ids`() {
        repository.preload(
            confirmed(1L, userId = 1L),
            confirmed(2L, userId = 2L),
            confirmed(3L, userId = 1L),
        )

        mockMvc().perform(
            get("/investments").header(AuthHeaders.AUTHENTICATED_USER_ID, "1"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total_count").value(2))
            .andExpect(jsonPath("$.investments[0].investment_id").value(3))
            .andExpect(jsonPath("$.investments[0].confirm_method").exists())
            .andExpect(jsonPath("$.investments[0].user_id").doesNotExist())
            .andExpect(jsonPath("$.investments[0].payment_id").doesNotExist())
    }

    @Test
    fun `getDetail denies non-owner with 403`() {
        repository.preload(confirmed(5L, userId = 9L))

        mockMvc().perform(
            get("/investments/5").header(AuthHeaders.AUTHENTICATED_USER_ID, "1"),
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `getMyInvestments without auth header returns 401`() {
        mockMvc().perform(get("/investments"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `adminCreate returns 201 and persists a confirmed investment`() {
        mockMvc().perform(
            post("/investments/admin")
                .contentType("application/json")
                .content("""{"user_id": 42, "amount": 30000, "type": "DEPOSIT"}"""),
        ).andExpect(status().isCreated)

        val saved = repository.saved.single()
        assert(saved.userId == 42L)
        assert(saved.amount == 30000)
    }

    private fun confirmed(id: Long, userId: Long): Investment =
        Investment.pending(userId, "I-$id", 50_000).copy(investmentId = id).confirm()

    private object NoopEventPublisher : EventPublisher {
        override fun publish(topic: String, key: String, eventType: String, payload: Any) = Unit
    }

    private class FakeInvestmentRepository : InvestmentRepository {
        val saved = mutableListOf<Investment>()
        private var nextId = 1L

        fun preload(vararg investments: Investment) {
            saved.addAll(investments)
        }

        override fun findByPaymentId(paymentId: String): Investment? =
            saved.firstOrNull { it.paymentId == paymentId }

        override fun findById(investmentId: Long): Investment? =
            saved.firstOrNull { it.investmentId == investmentId }

        override fun findByUserId(userId: Long, pageable: Pageable): Page<Investment> {
            val own = saved.filter { it.userId == userId }.sortedByDescending { it.investmentId }
            return PageImpl(own, pageable, own.size.toLong())
        }

        override fun findAll(pageable: Pageable): Page<Investment> {
            val all = saved.sortedByDescending { it.investmentId }
            return PageImpl(all, pageable, all.size.toLong())
        }

        override fun save(investment: Investment): Investment {
            val persisted = if (investment.investmentId == 0L) investment.copy(investmentId = nextId++) else investment
            saved += persisted
            return persisted
        }

        override fun confirmIfPending(paymentId: String, confirmedAt: java.time.LocalDateTime): Boolean = false
    }
}
