package devcoop.occount.inquiry.application.usecase.create

import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import java.time.LocalDateTime

data class CreateInquiryResponse(
    val inquiryId: Long,
    val status: InquiryStatus,
    val createdAt: LocalDateTime,
)
