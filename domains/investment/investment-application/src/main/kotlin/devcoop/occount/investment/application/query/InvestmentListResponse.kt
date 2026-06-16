package devcoop.occount.investment.application.query

import org.springframework.data.domain.Page

data class InvestmentListResponse(
    val investments: List<InvestmentResult>,
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
) {
    companion object {
        fun from(page: Page<InvestmentResult>): InvestmentListResponse {
            return InvestmentListResponse(
                investments = page.content,
                totalCount = page.totalElements,
                totalPages = page.totalPages,
                currentPage = page.number,
                pageSize = page.size,
            )
        }
    }
}
