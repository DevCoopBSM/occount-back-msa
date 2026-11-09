package devcoop.occount.inquiry.application.shared

data class InquiryListResponse(
    val inquiries: List<InquiryListItemResponse>,
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
)
