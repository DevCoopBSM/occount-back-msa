package devcoop.occount.inquiry.api.inquiry

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.core.common.auth.AuthPrincipalArgumentResolver
import devcoop.occount.inquiry.application.query.GetInquiryDetailQueryService
import devcoop.occount.inquiry.application.query.GetInquiryListQueryService
import devcoop.occount.inquiry.application.shared.InquiryListItemResponse
import devcoop.occount.inquiry.application.shared.InquiryListResponse
import devcoop.occount.inquiry.application.usecase.create.CreateInquiryRequest
import devcoop.occount.inquiry.application.usecase.create.CreateInquiryResponse
import devcoop.occount.inquiry.application.usecase.create.CreateInquiryUseCase
import devcoop.occount.inquiry.domain.inquiry.InquiryCategory
import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.LocalDateTime

/**
 * inquiry-api의 외부 JSON 계약이 SNAKE_CASE인지 고정한다.
 *
 * 실제 앱 설정(`spring.jackson.property-naming-strategy: SNAKE_CASE`)과 동일한 매퍼로
 * DispatcherServlet 직렬화 경로를 태워, 응답/요청 키가 snake_case임을 검증한다.
 * (member/suggestion 컨트롤러 테스트와 동일한 방식)
 */
@DisplayName("InquiryController SNAKE_CASE 계약 테스트")
class InquiryControllerContractTest {

    private val createInquiryUseCase = mock(CreateInquiryUseCase::class.java)
    private val getInquiryListQueryService = mock(GetInquiryListQueryService::class.java)
    private val getInquiryDetailQueryService = mock(GetInquiryDetailQueryService::class.java)

    // Kotlin non-null 파라미터에 Mockito.any()가 null을 반환하는 문제를 elvis로 우회한다.
    private fun anyPageable(): Pageable = Mockito.any(Pageable::class.java) ?: Pageable.unpaged()
    private fun anyCreateRequest(): CreateInquiryRequest =
        Mockito.any(CreateInquiryRequest::class.java)
            ?: CreateInquiryRequest("", "", InquiryCategory.PAYMENT)

    private fun mockMvc(): MockMvc {
        val controller = InquiryController(createInquiryUseCase, getInquiryListQueryService, getInquiryDetailQueryService)
        val objectMapper = jacksonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build()
        return MockMvcBuilders.standaloneSetup(controller)
            .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver(), AuthPrincipalArgumentResolver())
            .setMessageConverters(JacksonJsonHttpMessageConverter(objectMapper))
            .build()
    }

    @Test
    @DisplayName("목록 응답 필드 키가 snake_case다")
    fun `list response keys are snake_case`() {
        `when`(getInquiryListQueryService.getList(anyLong(), anyPageable())).thenReturn(
            InquiryListResponse(
                inquiries = listOf(
                    InquiryListItemResponse(
                        inquiryId = 1L,
                        title = "결제가 안돼요",
                        category = InquiryCategory.PAYMENT,
                        status = InquiryStatus.RECEIVED,
                        createdAt = LocalDateTime.of(2026, 4, 22, 10, 0),
                    ),
                ),
                totalCount = 1L,
                totalPages = 1,
                currentPage = 0,
                pageSize = 20,
            ),
        )

        mockMvc().perform(get("/inquiries").header(AuthHeaders.AUTHENTICATED_USER_ID, "17"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total_count").value(1))
            .andExpect(jsonPath("$.total_pages").value(1))
            .andExpect(jsonPath("$.current_page").value(0))
            .andExpect(jsonPath("$.page_size").value(20))
            .andExpect(jsonPath("$.inquiries[0].inquiry_id").value(1))
            .andExpect(jsonPath("$.inquiries[0].created_at").exists())
    }

    @Test
    @DisplayName("문의 제출 응답 필드 키가 snake_case다")
    fun `create response keys are snake_case`() {
        `when`(createInquiryUseCase.create(anyLong(), anyCreateRequest())).thenReturn(
            CreateInquiryResponse(
                inquiryId = 9L,
                status = InquiryStatus.RECEIVED,
                createdAt = LocalDateTime.of(2026, 4, 22, 10, 0),
            ),
        )

        mockMvc().perform(
            post("/inquiries")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "17")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"결제가 안돼요","content":"포인트 결제 시 오류가 발생합니다.","category":"PAYMENT"}"""),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.inquiry_id").value(9))
            .andExpect(jsonPath("$.created_at").exists())
    }
}
