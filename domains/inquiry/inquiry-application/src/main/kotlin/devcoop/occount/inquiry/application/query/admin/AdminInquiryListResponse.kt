package devcoop.occount.inquiry.application.query.admin

import devcoop.occount.inquiry.domain.inquiry.InquiryCategory
import devcoop.occount.inquiry.domain.inquiry.InquiryStatus
import java.time.LocalDateTime

data class AdminInquiryListResponse(
    val inquiries: List<AdminInquiryListItemResponse>,
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
)

data class AdminInquiryListItemResponse(
    val inquiryId: Long,
    val userId: Long,
    val title: String,
    val category: InquiryCategory,
    val status: InquiryStatus,
    val createdAt: LocalDateTime,
)
