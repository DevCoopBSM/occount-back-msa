package devcoop.occount.inquiry.application.query

import devcoop.occount.inquiry.application.exception.InquiryAccessDeniedException
import devcoop.occount.inquiry.application.exception.InquiryNotFoundException
import devcoop.occount.inquiry.application.output.InquiryRepository
import devcoop.occount.inquiry.application.shared.InquiryDetailResponse
import org.springframework.stereotype.Service

@Service
class GetInquiryDetailQueryService(
    private val inquiryRepository: InquiryRepository,
) {
    fun getDetail(userId: Long, inquiryId: Long): InquiryDetailResponse {
        val inquiry = inquiryRepository.findById(inquiryId)
            ?: throw InquiryNotFoundException()

        if (inquiry.userId != userId) {
            throw InquiryAccessDeniedException()
        }

        return InquiryDetailResponse(
            inquiryId = inquiry.id,
            title = inquiry.title,
            content = inquiry.content,
            category = inquiry.category,
            status = inquiry.status,
            createdAt = inquiry.createdAt,
            updatedAt = inquiry.updatedAt,
        )
    }
}
