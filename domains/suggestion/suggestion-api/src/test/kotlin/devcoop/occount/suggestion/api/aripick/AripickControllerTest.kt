package devcoop.occount.suggestion.api.aripick

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.suggestion.application.query.AripickFoodQueryService
import devcoop.occount.suggestion.application.query.AripickFoodSearchItemResponse
import devcoop.occount.suggestion.application.query.AripickFoodSearchResponse
import devcoop.occount.suggestion.application.query.AripickListResponse
import devcoop.occount.suggestion.application.query.AripickQueryService
import devcoop.occount.suggestion.application.query.AripickStatsResponse
import devcoop.occount.suggestion.application.shared.AripickResponse
import devcoop.occount.suggestion.application.usecase.aripick.AripickBlockedKeywordListResponse
import devcoop.occount.suggestion.application.usecase.aripick.AripickBlockedKeywordResponse
import devcoop.occount.suggestion.application.usecase.aripick.AripickCommandUseCase
import devcoop.occount.suggestion.application.usecase.aripick.AripickLikeToggleResponse
import devcoop.occount.suggestion.application.usecase.aripick.AripickPolicyUseCase
import devcoop.occount.suggestion.application.usecase.aripick.CreateAripickBlockedKeywordRequest
import devcoop.occount.suggestion.application.usecase.aripick.CreateAripickRequest
import devcoop.occount.suggestion.domain.aripick.AripickStatus
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.LocalDate

class AripickControllerTest {
    @Test
    fun `get list delegates to query service`() {
        val queryService = mock(AripickQueryService::class.java)
        val controller = controller(aripickQueryService = queryService)
        val expected = AripickListResponse(
            aripickItems = listOf(
                AripickResponse(
                    proposalId = 1L,
                    name = "제로콜라",
                    reason = "원함",
                    proposerId = 7L,
                    proposalDate = java.time.LocalDate.of(2026, 4, 22),
                    status = AripickStatus.검토중,
                    likeCount = 0,
                ),
            ),
        )
        `when`(queryService.getAllItems()).thenReturn(expected)

        val actual = controller.getAripickItems()

        assertSame(expected, actual)
        verify(queryService).getAllItems()
    }

    @Test
    fun `get single delegates to query service`() {
        val queryService = mock(AripickQueryService::class.java)
        val controller = controller(aripickQueryService = queryService)
        val expected = AripickResponse(
            proposalId = 1L,
            name = "제로콜라",
            reason = "원함",
            proposerId = 7L,
            proposalDate = java.time.LocalDate.of(2026, 4, 22),
            status = AripickStatus.검토중,
            likeCount = 1,
        )
        `when`(queryService.getItem(1L)).thenReturn(expected)

        val actual = controller.getAripickItem(1L)

        assertSame(expected, actual)
        verify(queryService).getItem(1L)
    }

    @Test
    fun `get stats delegates to query service`() {
        val queryService = mock(AripickQueryService::class.java)
        val controller = controller(aripickQueryService = queryService)
        val expected = AripickStatsResponse(
            totalProposals = 10,
            approved = 3,
            pending = 5,
            rejected = 2,
        )
        `when`(queryService.getStats()).thenReturn(expected)

        val actual = controller.getAripickStats()

        assertSame(expected, actual)
        verify(queryService).getStats()
    }

    @Test
    fun `create delegates to command use case with requester id`() {
        val commandUseCase = mock(AripickCommandUseCase::class.java)
        val controller = controller(aripickCommandUseCase = commandUseCase)
        val request = CreateAripickRequest(typeNSeq = 14116L, reason = "원함")
        val httpRequest = authenticatedRequest(7L)
        val expected = AripickResponse(
            proposalId = 1L,
            name = "제로콜라",
            reason = "원함",
            proposerId = 7L,
            proposalDate = java.time.LocalDate.of(2026, 4, 22),
            status = AripickStatus.검토중,
            likeCount = 0,
        )
        `when`(commandUseCase.create(request, 7L)).thenReturn(expected)

        val actual = controller.createAripickItem(request, httpRequest)

        assertSame(expected, actual)
        verify(commandUseCase).create(request, 7L)
    }

    @Test
    fun `approve delegates to command use case`() {
        val commandUseCase = mock(AripickCommandUseCase::class.java)
        val controller = controller(aripickCommandUseCase = commandUseCase)
        val expected = AripickResponse(
            proposalId = 1L,
            name = "제로콜라",
            reason = "원함",
            proposerId = 7L,
            proposalDate = java.time.LocalDate.of(2026, 4, 22),
            status = AripickStatus.승인됨,
            likeCount = 0,
        )
        `when`(commandUseCase.approve(1L)).thenReturn(expected)

        val actual = controller.approveAripickItem(1L)

        assertSame(expected, actual)
        verify(commandUseCase).approve(1L)
    }

    @Test
    fun `reject delegates to command use case`() {
        val commandUseCase = mock(AripickCommandUseCase::class.java)
        val controller = controller(aripickCommandUseCase = commandUseCase)
        val expected = AripickResponse(
            proposalId = 1L,
            name = "제로콜라",
            reason = "원함",
            proposerId = 7L,
            proposalDate = java.time.LocalDate.of(2026, 4, 22),
            status = AripickStatus.거절됨,
            likeCount = 0,
        )
        `when`(commandUseCase.reject(1L)).thenReturn(expected)

        val actual = controller.rejectAripickItem(1L)

        assertSame(expected, actual)
        verify(commandUseCase).reject(1L)
    }

    @Test
    fun `pending delegates to command use case`() {
        val commandUseCase = mock(AripickCommandUseCase::class.java)
        val controller = controller(aripickCommandUseCase = commandUseCase)
        val expected = AripickResponse(
            proposalId = 1L,
            name = "제로콜라",
            reason = "원함",
            proposerId = 7L,
            proposalDate = java.time.LocalDate.of(2026, 4, 22),
            status = AripickStatus.검토중,
            likeCount = 0,
        )
        `when`(commandUseCase.pending(1L)).thenReturn(expected)

        val actual = controller.pendingAripickItem(1L)

        assertSame(expected, actual)
        verify(commandUseCase).pending(1L)
    }

    @Test
    fun `delete delegates to command use case with requester id`() {
        val commandUseCase = mock(AripickCommandUseCase::class.java)
        val controller = controller(aripickCommandUseCase = commandUseCase)
        val httpRequest = authenticatedRequest(7L)

        controller.deleteAripickItem(1L, httpRequest)

        verify(commandUseCase).delete(1L, 7L)
    }

    @Test
    fun `delete admin delegates to admin delete use case`() {
        val commandUseCase = mock(AripickCommandUseCase::class.java)
        val controller = controller(aripickCommandUseCase = commandUseCase)

        controller.deleteAripickItemAsAdmin(1L)

        verify(commandUseCase).deleteAsAdmin(1L)
    }

    @Test
    fun `toggle like delegates to command use case with requester id`() {
        val commandUseCase = mock(AripickCommandUseCase::class.java)
        val controller = controller(aripickCommandUseCase = commandUseCase)
        val httpRequest = authenticatedRequest(7L)
        val expected = AripickLikeToggleResponse(
            proposalId = 1L,
            liked = true,
            likeCount = 3,
        )
        `when`(commandUseCase.toggleLike(1L, 7L)).thenReturn(expected)

        val actual = controller.toggleLike(1L, httpRequest)

        assertSame(expected, actual)
        verify(commandUseCase).toggleLike(1L, 7L)
    }

    @Test
    fun `foods search delegates to food query service`() {
        val foodQueryService = mock(AripickFoodQueryService::class.java)
        val controller = controller(aripickFoodQueryService = foodQueryService)
        val expected = AripickFoodSearchResponse(
            items = listOf(
                AripickFoodSearchItemResponse(
                    typeNSeq = 14116L,
                    name = "신라면 큰사발면",
                    company = "㈜농심",
                    kcalInfo = "347.37kcal / 114g",
                ),
            ),
        )
        `when`(foodQueryService.search("신라면")).thenReturn(expected)

        val actual = controller.searchFoods("신라면")

        assertSame(expected, actual)
        verify(foodQueryService).search("신라면")
    }

    @Test
    fun `blocked keyword list delegates to policy use case`() {
        val policyUseCase = mock(AripickPolicyUseCase::class.java)
        val controller = controller(aripickPolicyUseCase = policyUseCase)
        val expected = AripickBlockedKeywordListResponse(
            keywords = listOf(
                AripickBlockedKeywordResponse(
                    keywordId = 1L,
                    keyword = "에너지",
                    registeredDate = LocalDate.of(2026, 4, 22),
                ),
            ),
        )
        `when`(policyUseCase.getBlockedKeywords()).thenReturn(expected)

        val actual = controller.getBlockedKeywords()

        assertSame(expected, actual)
        verify(policyUseCase).getBlockedKeywords()
    }

    @Test
    fun `block keyword delegates to policy use case`() {
        val policyUseCase = mock(AripickPolicyUseCase::class.java)
        val controller = controller(aripickPolicyUseCase = policyUseCase)
        val request = CreateAripickBlockedKeywordRequest(keyword = "에너지")
        val expected = AripickBlockedKeywordResponse(
            keywordId = 1L,
            keyword = "에너지",
            registeredDate = LocalDate.of(2026, 4, 22),
        )
        `when`(policyUseCase.blockKeyword(request)).thenReturn(expected)

        val actual = controller.blockKeyword(request)

        assertSame(expected, actual)
        verify(policyUseCase).blockKeyword(request)
    }

    @Test
    fun `unblock keyword delegates to policy use case`() {
        val policyUseCase = mock(AripickPolicyUseCase::class.java)
        val controller = controller(aripickPolicyUseCase = policyUseCase)

        controller.unblockKeyword(1L)

        verify(policyUseCase).unblockKeyword(1L)
    }

    private fun authenticatedRequest(userId: Long): HttpServletRequest {
        val request = mock(HttpServletRequest::class.java)
        `when`(request.getHeader(AuthHeaders.AUTHENTICATED_USER_ID)).thenReturn(userId.toString())
        return request
    }

    private fun controller(
        aripickQueryService: AripickQueryService = mock(AripickQueryService::class.java),
        aripickFoodQueryService: AripickFoodQueryService = mock(AripickFoodQueryService::class.java),
        aripickCommandUseCase: AripickCommandUseCase = mock(AripickCommandUseCase::class.java),
        aripickPolicyUseCase: AripickPolicyUseCase = mock(AripickPolicyUseCase::class.java),
    ): AripickController {
        return AripickController(
            aripickQueryService = aripickQueryService,
            aripickFoodQueryService = aripickFoodQueryService,
            aripickCommandUseCase = aripickCommandUseCase,
            aripickPolicyUseCase = aripickPolicyUseCase,
        )
    }
}
