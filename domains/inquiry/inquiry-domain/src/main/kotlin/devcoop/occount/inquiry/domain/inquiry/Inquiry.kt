package devcoop.occount.inquiry.domain.inquiry

import java.time.LocalDateTime

data class Inquiry(
    val id: Long = 0L,
    val userId: Long,
    val title: String,
    val content: String,
    val category: InquiryCategory,
    val status: InquiryStatus = InquiryStatus.RECEIVED,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
