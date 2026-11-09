package devcoop.occount.payment.application.query.chargelog

import org.springframework.data.domain.Page

data class ChargeHistoryListResponse(
    val charges: List<ChargeLogResult>,
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
) {
    companion object {
        fun from(page: Page<ChargeLogResult>): ChargeHistoryListResponse {
            return ChargeHistoryListResponse(
                charges = page.content,
                totalCount = page.totalElements,
                totalPages = page.totalPages,
                currentPage = page.number,
                pageSize = page.size,
            )
        }
    }
}
