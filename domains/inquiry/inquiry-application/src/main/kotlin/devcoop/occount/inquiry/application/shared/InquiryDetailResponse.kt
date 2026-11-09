package devcoop.occount.inquiry.application.shared

import devcoop.occount.inquiry.domain.inquiry.InquiryCategory
import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import java.time.LocalDateTime

data class InquiryDetailResponse(
    val inquiryId: Long,
    val title: String,
    val content: String,
    val category: InquiryCategory,
    val status: InquiryStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
