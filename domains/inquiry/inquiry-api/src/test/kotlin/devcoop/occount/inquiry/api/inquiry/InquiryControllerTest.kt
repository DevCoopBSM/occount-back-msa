package devcoop.occount.inquiry.api.inquiry

import devcoop.occount.core.common.auth.AuthPrincipal
import devcoop.occount.inquiry.application.query.GetInquiryDetailQueryService
import devcoop.occount.inquiry.application.query.GetInquiryListQueryService
import devcoop.occount.inquiry.application.shared.InquiryDetailResponse
import devcoop.occount.inquiry.application.shared.InquiryListItemResponse
import devcoop.occount.inquiry.application.shared.InquiryListResponse
import devcoop.occount.inquiry.application.usecase.create.CreateInquiryRequest
import devcoop.occount.inquiry.application.usecase.create.CreateInquiryResponse
import devcoop.occount.inquiry.application.usecase.create.CreateInquiryUseCase
import devcoop.occount.inquiry.domain.inquiry.InquiryCategory
import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.time.LocalDateTime

@DisplayName("InquiryController 단위 테스트")
class InquiryControllerTest {

    private lateinit var createInquiryUseCase: CreateInquiryUseCase
    private lateinit var getInquiryListQueryService: GetInquiryListQueryService
    private lateinit var getInquiryDetailQueryService: GetInquiryDetailQueryService
    private lateinit var controller: InquiryController

    @BeforeEach
    fun setUp() {
        createInquiryUseCase = mock(CreateInquiryUseCase::class.java)
        getInquiryListQueryService = mock(GetInquiryListQueryService::class.java)
        getInquiryDetailQueryService = mock(GetInquiryDetailQueryService::class.java)
        controller = InquiryController(createInquiryUseCase, getInquiryListQueryService, getInquiryDetailQueryService)
    }

    private fun authPrincipal(userId: Long = 17L) = AuthPrincipal(userId)

    @Test
    @DisplayName("문의 제출 성공 시 CreateInquiryResponse를 반환한다")
    fun `createInquiry returns response from use case`() {
        val request = CreateInquiryRequest(
            title = "결제가 안돼요",
            content = "포인트 결제 시 오류가 발생합니다.",
            category = InquiryCategory.PAYMENT,
        )
        val expected = CreateInquiryResponse(
            inquiryId = 1L,
            status = InquiryStatus.RECEIVED,
            createdAt = LocalDateTime.now(),
        )
        `when`(createInquiryUseCase.create(17L, request)).thenReturn(expected)

        val actual = controller.createInquiry(request, authPrincipal(17L))

        assertSame(expected, actual)
        verify(createInquiryUseCase).create(17L, request)
    }

    @Test
    @DisplayName("내 문의 목록 조회 시 InquiryListResponse를 반환한다")
    fun `getInquiryList returns response from query service`() {
        val pageable = PageRequest.of(0, 20)
        val sanitizedPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        val expected = InquiryListResponse(
            inquiries = listOf(
                InquiryListItemResponse(
                    inquiryId = 1L,
                    title = "결제가 안돼요",
                    category = InquiryCategory.PAYMENT,
                    status = InquiryStatus.RECEIVED,
                    createdAt = LocalDateTime.now(),
                ),
            ),
            totalCount = 1L,
            totalPages = 1,
            currentPage = 0,
            pageSize = 20,
        )
        `when`(getInquiryListQueryService.getList(17L, sanitizedPageable)).thenReturn(expected)

        val actual = controller.getInquiryList(authPrincipal(17L), pageable)

        assertSame(expected, actual)
        verify(getInquiryListQueryService).getList(17L, sanitizedPageable)
    }

    @Test
    @DisplayName("문의 상세 조회 시 InquiryDetailResponse를 반환한다")
    fun `getInquiryDetail returns response from query service`() {
        val now = LocalDateTime.now()
        val expected = InquiryDetailResponse(
            inquiryId = 1L,
            title = "결제가 안돼요",
            content = "포인트 결제 시 오류가 발생합니다.",
            category = InquiryCategory.PAYMENT,
            status = InquiryStatus.RECEIVED,
            createdAt = now,
            updatedAt = now,
        )
        `when`(getInquiryDetailQueryService.getDetail(17L, 1L)).thenReturn(expected)

        val actual = controller.getInquiryDetail(1L, authPrincipal(17L))

        assertSame(expected, actual)
        verify(getInquiryDetailQueryService).getDetail(17L, 1L)
    }
}
