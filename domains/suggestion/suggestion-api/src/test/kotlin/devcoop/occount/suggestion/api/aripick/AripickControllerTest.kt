package devcoop.occount.suggestion.api.aripick

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.suggestion.api.support.ApiAdviceHandler
import devcoop.occount.suggestion.application.query.AripickFoodQueryService
import devcoop.occount.suggestion.application.query.AripickFoodSearchItemResponse
import devcoop.occount.suggestion.application.query.AripickFoodSearchResponse
import devcoop.occount.suggestion.application.query.AripickListResponse
import devcoop.occount.suggestion.application.query.AripickQueryService
import devcoop.occount.suggestion.application.query.AripickStatsResponse
import devcoop.occount.suggestion.application.shared.AripickBlockedKeywordListResponse
import devcoop.occount.suggestion.application.shared.AripickBlockedKeywordResponse
import devcoop.occount.suggestion.application.shared.AripickResponse
import devcoop.occount.suggestion.application.usecase.aripick.AripickCommandUseCase
import devcoop.occount.suggestion.application.usecase.aripick.AripickLikeToggleResponse
import devcoop.occount.suggestion.application.usecase.aripick.AripickPolicyUseCase
import devcoop.occount.suggestion.application.usecase.aripick.CreateAripickBlockedKeywordRequest
import devcoop.occount.suggestion.application.usecase.aripick.CreateAripickRequest
import devcoop.occount.suggestion.domain.aripick.AripickStatus
import java.time.LocalDate
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class AripickControllerTest {
    @Test
    fun `get list returns aripick items`() {
        val queryService = mock(AripickQueryService::class.java)
        `when`(queryService.getAllItems()).thenReturn(
            AripickListResponse(
                aripickItems = listOf(aripickResponse()),
            ),
        )
        val mockMvc = mockMvc(aripickQueryService = queryService)

        mockMvc.perform(get("/ari-pick"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.aripickItems.length()").value(1))
            .andExpect(jsonPath("$.aripickItems[0].proposalId").value(1))
    }

    @Test
    fun `get single returns one aripick item`() {
        val queryService = mock(AripickQueryService::class.java)
        `when`(queryService.getItem(1L)).thenReturn(aripickResponse())
        val mockMvc = mockMvc(aripickQueryService = queryService)

        mockMvc.perform(get("/ari-pick/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.proposalId").value(1))
            .andExpect(jsonPath("$.name").value("제로콜라"))
    }

    @Test
    fun `get stats returns summary`() {
        val queryService = mock(AripickQueryService::class.java)
        `when`(queryService.getStats()).thenReturn(
            AripickStatsResponse(
                totalProposals = 10,
                approved = 3,
                pending = 5,
                rejected = 2,
            ),
        )
        val mockMvc = mockMvc(aripickQueryService = queryService)

        mockMvc.perform(get("/ari-pick/stats"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalProposals").value(10))
            .andExpect(jsonPath("$.approved").value(3))
    }

    @Test
    fun `foods search returns matched foods`() {
        val foodQueryService = mock(AripickFoodQueryService::class.java)
        `when`(foodQueryService.search("신라면")).thenReturn(
            AripickFoodSearchResponse(
                items = listOf(
                    AripickFoodSearchItemResponse(
                        typeNSeq = 14116L,
                        name = "신라면 큰사발면",
                        company = "농심",
                        kcalInfo = "347kcal",
                    ),
                ),
            ),
        )
        val mockMvc = mockMvc(aripickFoodQueryService = foodQueryService)

        mockMvc.perform(get("/ari-pick/foods").param("keyword", "신라면"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].typeNSeq").value(14116))
    }

    @Test
    fun `blocked keyword list returns keywords`() {
        val policyUseCase = mock(AripickPolicyUseCase::class.java)
        `when`(policyUseCase.getBlockedKeywords()).thenReturn(
            AripickBlockedKeywordListResponse(
                keywords = listOf(
                    AripickBlockedKeywordResponse(
                        keywordId = 1L,
                        keyword = "에너지",
                        registeredDate = LocalDate.of(2026, 4, 22),
                    ),
                ),
            ),
        )
        val mockMvc = mockMvc(aripickPolicyUseCase = policyUseCase)

        mockMvc.perform(get("/ari-pick/blocked-keywords"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.keywords.length()").value(1))
            .andExpect(jsonPath("$.keywords[0].keyword").value("에너지"))
    }

    @Test
    fun `block keyword returns created`() {
        val policyUseCase = mock(AripickPolicyUseCase::class.java)
        val request = CreateAripickBlockedKeywordRequest(keyword = "에너지")
        `when`(policyUseCase.blockKeyword(request)).thenReturn(
            AripickBlockedKeywordResponse(
                keywordId = 1L,
                keyword = "에너지",
                registeredDate = LocalDate.of(2026, 4, 22),
            ),
        )
        val mockMvc = mockMvc(aripickPolicyUseCase = policyUseCase)

        mockMvc.perform(
            post("/ari-pick/blocked-keywords")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"keyword":"에너지"}"""),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.keywordId").value(1))
    }

    @Test
    fun `unblock keyword returns no content`() {
        val mockMvc = mockMvc(aripickPolicyUseCase = mock(AripickPolicyUseCase::class.java))

        mockMvc.perform(delete("/ari-pick/blocked-keywords/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `create aripick returns created when user header exists`() {
        val commandUseCase = mock(AripickCommandUseCase::class.java)
        `when`(commandUseCase.create(CreateAripickRequest(14116L, "원함"), 7L)).thenReturn(aripickResponse())
        val mockMvc = mockMvc(aripickCommandUseCase = commandUseCase)

        mockMvc.perform(
            post("/ari-pick")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"typeNSeq":14116,"reason":"원함"}"""),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.proposalId").value(1))
    }

    @Test
    fun `approve reject pending return ok`() {
        val commandUseCase = mock(AripickCommandUseCase::class.java)
        `when`(commandUseCase.approve(1L)).thenReturn(aripickResponse(status = AripickStatus.APPROVED))
        `when`(commandUseCase.reject(1L)).thenReturn(aripickResponse(status = AripickStatus.REJECTED))
        `when`(commandUseCase.pending(1L)).thenReturn(aripickResponse(status = AripickStatus.PENDING))
        val mockMvc = mockMvc(aripickCommandUseCase = commandUseCase)

        mockMvc.perform(patch("/ari-pick/1/approve")).andExpect(status().isOk)
        mockMvc.perform(patch("/ari-pick/1/reject")).andExpect(status().isOk)
        mockMvc.perform(patch("/ari-pick/1/pending")).andExpect(status().isOk)
    }

    @Test
    fun `delete aripick returns no content when user header exists`() {
        val mockMvc = mockMvc(aripickCommandUseCase = mock(AripickCommandUseCase::class.java))

        mockMvc.perform(
            delete("/ari-pick/1")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7"),
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `delete aripick as admin returns no content`() {
        val mockMvc = mockMvc(aripickCommandUseCase = mock(AripickCommandUseCase::class.java))

        mockMvc.perform(delete("/ari-pick/1/admin"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `toggle like returns updated state`() {
        val commandUseCase = mock(AripickCommandUseCase::class.java)
        `when`(commandUseCase.toggleLike(1L, 7L)).thenReturn(
            AripickLikeToggleResponse(
                proposalId = 1L,
                liked = true,
                likeCount = 3,
            ),
        )
        val mockMvc = mockMvc(aripickCommandUseCase = commandUseCase)

        mockMvc.perform(
            post("/ari-pick/1/like")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.liked").value(true))
            .andExpect(jsonPath("$.likeCount").value(3))
    }

    private fun mockMvc(
        aripickQueryService: AripickQueryService = mock(AripickQueryService::class.java),
        aripickFoodQueryService: AripickFoodQueryService = mock(AripickFoodQueryService::class.java),
        aripickCommandUseCase: AripickCommandUseCase = mock(AripickCommandUseCase::class.java),
        aripickPolicyUseCase: AripickPolicyUseCase = mock(AripickPolicyUseCase::class.java),
    ): MockMvc {
        val controller = AripickController(
            aripickQueryService = aripickQueryService,
            aripickFoodQueryService = aripickFoodQueryService,
            aripickCommandUseCase = aripickCommandUseCase,
            aripickPolicyUseCase = aripickPolicyUseCase,
        )
        val converter = JacksonJsonHttpMessageConverter()
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(ApiAdviceHandler())
            .setMessageConverters(converter)
            .build()
    }

    private fun aripickResponse(status: AripickStatus = AripickStatus.PENDING): AripickResponse {
        return AripickResponse(
            proposalId = 1L,
            name = "제로콜라",
            reason = "원함",
            proposerId = 7L,
            proposalDate = LocalDate.of(2026, 4, 22),
            status = status,
            likeCount = 0,
        )
    }
}
