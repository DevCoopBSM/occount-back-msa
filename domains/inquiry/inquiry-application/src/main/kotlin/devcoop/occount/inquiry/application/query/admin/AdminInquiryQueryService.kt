package devcoop.occount.inquiry.application.query.admin

import devcoop.occount.inquiry.application.exception.InquiryNotFoundException
import devcoop.occount.inquiry.application.output.InquiryRepository
import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * 관리자 전용 문의 조회. 본인 소유 여부와 무관하게 전체 문의를 조회한다.
 * 인가는 게이트웨이의 inquiries admin 하위 경로 규칙(ADMIN_ONLY)에서만 강제한다.
 */
@Service
class AdminInquiryQueryService(
    private val inquiryRepository: InquiryRepository,
) {
    fun getList(status: InquiryStatus?, pageable: Pageable): AdminInquiryListResponse {
        val page = inquiryRepository.findPage(status, pageable)
        val items = page.content.map {
            AdminInquiryListItemResponse(
                inquiryId = it.id,
                userId = it.userId,
                title = it.title,
                category = it.category,
                status = it.status,
                createdAt = it.createdAt,
            )
        }
        return AdminInquiryListResponse(
            inquiries = items,
            totalCount = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            pageSize = page.size,
        )
    }

    fun getDetail(inquiryId: Long): AdminInquiryDetailResponse {
        val inquiry = inquiryRepository.findById(inquiryId)
            ?: throw InquiryNotFoundException()

        return AdminInquiryDetailResponse(
            inquiryId = inquiry.id,
            userId = inquiry.userId,
            title = inquiry.title,
            content = inquiry.content,
            category = inquiry.category,
            status = inquiry.status,
            createdAt = inquiry.createdAt,
            updatedAt = inquiry.updatedAt,
        )
    }
}
