package devcoop.occount.inquiry.application.usecase.create

import devcoop.occount.inquiry.application.output.InquiryRepository
import devcoop.occount.inquiry.domain.inquiry.Inquiry
import org.springframework.stereotype.Service

@Service
class CreateInquiryUseCase(
    private val inquiryRepository: InquiryRepository,
) {
    fun create(userId: Long, request: CreateInquiryRequest): CreateInquiryResponse {
        val inquiry = Inquiry(
            userId = userId,
            title = request.title,
            content = request.content,
            category = request.category,
        )
        val saved = inquiryRepository.save(inquiry)
        return CreateInquiryResponse(
            inquiryId = saved.id,
            status = saved.status,
            createdAt = saved.createdAt,
        )
    }
}
