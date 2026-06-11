package devcoop.occount.inquiry.api.inquiry

import devcoop.occount.inquiry.application.query.admin.AdminInquiryDetailResponse
import devcoop.occount.inquiry.application.query.admin.AdminInquiryListItemResponse
import devcoop.occount.inquiry.application.query.admin.AdminInquiryListResponse
import devcoop.occount.inquiry.application.query.admin.AdminInquiryQueryService
import devcoop.occount.inquiry.domain.inquiry.InquiryCategory
import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.LocalDateTime

@DisplayName("AdminInquiryController SNAKE_CASE 계약 테스트")
class AdminInquiryControllerTest {

    private val adminInquiryQueryService = mock(AdminInquiryQueryService::class.java)

    private fun anyPageable(): Pageable = Mockito.any(Pageable::class.java) ?: Pageable.unpaged()

    private fun mockMvc(): MockMvc {
        val controller = AdminInquiryController(adminInquiryQueryService)
        val objectMapper = jacksonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build()
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(devcoop.occount.inquiry.api.support.ApiAdviceHandler())
            .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
            .setMessageConverters(org.springframework.http.converter.json.JacksonJsonHttpMessageConverter(objectMapper))
            .build()
    }

    @Test
    @DisplayName("전체 목록 응답 키가 snake_case이고 작성자 user_id를 노출한다")
    fun `admin list response keys are snake_case`() {
        `when`(adminInquiryQueryService.getList(isNull(), anyPageable())).thenReturn(
            AdminInquiryListResponse(
                inquiries = listOf(
                    AdminInquiryListItemResponse(
                        inquiryId = 1L,
                        userId = 42L,
                        title = "결제가 안돼요",
                        category = InquiryCategory.PAYMENT,
                        status = InquiryStatus.RECEIVED,
                        createdAt = LocalDateTime.of(2026, 6, 11, 9, 0),
                    ),
                ),
                totalCount = 1L,
                totalPages = 1,
                currentPage = 0,
                pageSize = 10,
            ),
        )

        mockMvc().perform(get("/inquiries/admin"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total_count").value(1))
            .andExpect(jsonPath("$.total_pages").value(1))
            .andExpect(jsonPath("$.current_page").value(0))
            .andExpect(jsonPath("$.page_size").value(10))
            .andExpect(jsonPath("$.inquiries[0].inquiry_id").value(1))
            .andExpect(jsonPath("$.inquiries[0].user_id").value(42))
            .andExpect(jsonPath("$.inquiries[0].created_at").exists())
    }

    @Test
    @DisplayName("status 쿼리 파라미터가 쿼리 서비스로 전달된다")
    fun `forwards status filter to query service`() {
        `when`(adminInquiryQueryService.getList(eq(InquiryStatus.RECEIVED), anyPageable())).thenReturn(
            AdminInquiryListResponse(emptyList(), 0L, 0, 0, 10),
        )

        mockMvc().perform(get("/inquiries/admin").param("status", "RECEIVED"))
            .andExpect(status().isOk)

        verify(adminInquiryQueryService).getList(eq(InquiryStatus.RECEIVED), anyPageable())
    }

    @Test
    @DisplayName("page size를 100으로 제한한다")
    fun `caps page size at 100`() {
        `when`(adminInquiryQueryService.getList(isNull(), anyPageable())).thenReturn(
            AdminInquiryListResponse(emptyList(), 0L, 0, 0, 100),
        )

        mockMvc().perform(get("/inquiries/admin").param("size", "500"))
            .andExpect(status().isOk)

        val captor = ArgumentCaptor.forClass(Pageable::class.java)
        verify(adminInquiryQueryService).getList(isNull(), captor.capture() ?: Pageable.unpaged())
        assertEquals(100, captor.value.pageSize)
    }

    @Test
    @DisplayName("잘못된 status 값은 400으로 거절한다")
    fun `invalid status returns 400`() {
        mockMvc().perform(get("/inquiries/admin").param("status", "NOPE"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("상세 응답 키가 snake_case이고 본문과 user_id를 노출한다")
    fun `admin detail response keys are snake_case`() {
        `when`(adminInquiryQueryService.getDetail(anyLong())).thenReturn(
            AdminInquiryDetailResponse(
                inquiryId = 7L,
                userId = 55L,
                title = "결제가 안돼요",
                content = "포인트 결제 시 오류가 발생합니다.",
                category = InquiryCategory.PAYMENT,
                status = InquiryStatus.IN_PROGRESS,
                createdAt = LocalDateTime.of(2026, 6, 11, 9, 0),
                updatedAt = LocalDateTime.of(2026, 6, 11, 10, 0),
            ),
        )

        mockMvc().perform(get("/inquiries/admin/7"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.inquiry_id").value(7))
            .andExpect(jsonPath("$.user_id").value(55))
            .andExpect(jsonPath("$.content").value("포인트 결제 시 오류가 발생합니다."))
            .andExpect(jsonPath("$.created_at").exists())
            .andExpect(jsonPath("$.updated_at").exists())
    }
}
