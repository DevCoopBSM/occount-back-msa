package devcoop.occount.investment.application.query

import org.springframework.data.domain.Page

data class AdminInvestmentListResponse(
    val investments: List<AdminInvestmentResult>,
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
) {
    companion object {
        fun from(page: Page<AdminInvestmentResult>): AdminInvestmentListResponse {
            return AdminInvestmentListResponse(
                investments = page.content,
                totalCount = page.totalElements,
                totalPages = page.totalPages,
                currentPage = page.number,
                pageSize = page.size,
            )
        }
    }
}
