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
    val updatedAt: LocalDateTime = createdAt,
) {
    init {
        require(userId > 0) { "userId must be positive" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(content.isNotBlank()) { "content must not be blank" }
    }
}
