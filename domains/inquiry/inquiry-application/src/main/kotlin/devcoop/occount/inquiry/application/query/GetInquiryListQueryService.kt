package devcoop.occount.inquiry.application.query

import devcoop.occount.inquiry.application.output.InquiryRepository
import devcoop.occount.inquiry.application.shared.InquiryListItemResponse
import devcoop.occount.inquiry.application.shared.InquiryListResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GetInquiryListQueryService(
    private val inquiryRepository: InquiryRepository,
) {
    fun getList(userId: Long, pageable: Pageable): InquiryListResponse {
        val page = inquiryRepository.findPageByUserId(userId, pageable)
        val items = page.content.map {
            InquiryListItemResponse(
                inquiryId = it.id,
                title = it.title,
                category = it.category,
                status = it.status,
                createdAt = it.createdAt,
            )
        }
        return InquiryListResponse(
            inquiries = items,
            totalCount = page.totalElements,
            totalPages = page.totalPages,
            currentPage = page.number,
            pageSize = page.size,
        )
    }
}
