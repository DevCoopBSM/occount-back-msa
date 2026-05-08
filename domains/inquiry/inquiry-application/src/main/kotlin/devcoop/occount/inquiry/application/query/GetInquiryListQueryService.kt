package devcoop.occount.inquiry.application.query

import devcoop.occount.inquiry.application.output.InquiryRepository
import devcoop.occount.inquiry.application.shared.InquiryListItemResponse
import devcoop.occount.inquiry.application.shared.InquiryListResponse
import org.springframework.stereotype.Service

@Service
class GetInquiryListQueryService(
    private val inquiryRepository: InquiryRepository,
) {
    fun getList(userId: Long): InquiryListResponse {
        val items = inquiryRepository.findAllByUserId(userId)
            .map {
                InquiryListItemResponse(
                    inquiryId = it.id,
                    title = it.title,
                    category = it.category,
                    status = it.status,
                    createdAt = it.createdAt,
                )
            }
        return InquiryListResponse(items)
    }
}
