package devcoop.occount.order.application.shared

import org.springframework.data.domain.Page

data class OrderHistoryListResponse(
    val orders: List<OrderHistoryItem>,
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
) {
    companion object {
        fun from(page: Page<OrderHistoryItem>): OrderHistoryListResponse {
            return OrderHistoryListResponse(
                orders = page.content,
                totalCount = page.totalElements,
                totalPages = page.totalPages,
                currentPage = page.number,
                pageSize = page.size,
            )
        }
    }
}
