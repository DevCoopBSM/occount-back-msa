package devcoop.occount.inquiry.application.query.admin

import devcoop.occount.inquiry.application.exception.InquiryNotFoundException
import devcoop.occount.inquiry.application.output.InquiryRepository
import devcoop.occount.inquiry.domain.inquiry.Inquiry
import devcoop.occount.inquiry.domain.inquiry.InquiryCategory
import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import org.junit.jupiter.api.DisplayName
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminInquiryQueryServiceTest {

    private fun inquiry(
        id: Long,
        userId: Long = 100L,
        status: InquiryStatus = InquiryStatus.RECEIVED,
        category: InquiryCategory = InquiryCategory.PAYMENT,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 6, 11, 9, 0),
        content: String = "본문",
    ) = Inquiry(
        id = id,
        userId = userId,
        title = "문의 $id",
        content = content,
        category = category,
        status = status,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    @Test
    @DisplayName("status가 null이면 전체 문의를 페이징 메타와 함께 반환한다")
    fun `returns all inquiries with pagination meta when status is null`() {
        val inquiries = (1L..25L).map { inquiry(id = it) }
        val service = AdminInquiryQueryService(FakeInquiryRepository(inquiries))

        val response = service.getList(null, PageRequest.of(0, 10))

        assertEquals(10, response.inquiries.size)
        assertEquals(25L, response.totalCount)
        assertEquals(3, response.totalPages)
        assertEquals(0, response.currentPage)
        assertEquals(10, response.pageSize)
    }

    @Test
    @DisplayName("status를 지정하면 해당 상태의 문의만 반환한다")
    fun `filters by status`() {
        val inquiries = listOf(
            inquiry(id = 1L, status = InquiryStatus.RECEIVED),
            inquiry(id = 2L, status = InquiryStatus.IN_PROGRESS),
            inquiry(id = 3L, status = InquiryStatus.RECEIVED),
            inquiry(id = 4L, status = InquiryStatus.COMPLETED),
        )
        val service = AdminInquiryQueryService(FakeInquiryRepository(inquiries))

        val response = service.getList(InquiryStatus.RECEIVED, PageRequest.of(0, 10))

        assertEquals(2L, response.totalCount)
        assertEquals(setOf(InquiryStatus.RECEIVED), response.inquiries.map { it.status }.toSet())
        assertEquals(listOf(1L, 3L), response.inquiries.map { it.inquiryId })
    }

    @Test
    @DisplayName("목록 항목에 작성자 userId가 포함된다")
    fun `list item includes author user id`() {
        val service = AdminInquiryQueryService(FakeInquiryRepository(listOf(inquiry(id = 1L, userId = 42L))))

        val item = service.getList(null, PageRequest.of(0, 10)).inquiries.single()

        assertEquals(1L, item.inquiryId)
        assertEquals(42L, item.userId)
    }

    @Test
    @DisplayName("어드민 상세는 소유자가 아니어도 본문과 userId를 반환한다")
    fun `detail returns content and user id regardless of ownership`() {
        val service = AdminInquiryQueryService(
            FakeInquiryRepository(listOf(inquiry(id = 7L, userId = 55L, content = "결제 오류 상세"))),
        )

        val detail = service.getDetail(7L)

        assertEquals(7L, detail.inquiryId)
        assertEquals(55L, detail.userId)
        assertEquals("결제 오류 상세", detail.content)
    }

    @Test
    @DisplayName("존재하지 않는 문의 상세 조회 시 InquiryNotFound가 발생한다")
    fun `detail throws when inquiry not found`() {
        val service = AdminInquiryQueryService(FakeInquiryRepository(emptyList()))

        assertFailsWith<InquiryNotFoundException> {
            service.getDetail(999L)
        }
    }

    private class FakeInquiryRepository(initial: List<Inquiry>) : InquiryRepository {
        private val store = initial.toList()

        override fun save(inquiry: Inquiry): Inquiry = inquiry

        override fun findPageByUserId(userId: Long, pageable: Pageable): Page<Inquiry> =
            paginate(store.filter { it.userId == userId }, pageable)

        override fun findById(id: Long): Inquiry? = store.firstOrNull { it.id == id }

        override fun findPage(status: InquiryStatus?, pageable: Pageable): Page<Inquiry> {
            val filtered = if (status == null) store else store.filter { it.status == status }
            return paginate(filtered, pageable)
        }

        private fun paginate(source: List<Inquiry>, pageable: Pageable): Page<Inquiry> {
            val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(source.size)
            val to = (from + pageable.pageSize).coerceAtMost(source.size)
            return PageImpl(source.subList(from, to), pageable, source.size.toLong())
        }
    }
}
