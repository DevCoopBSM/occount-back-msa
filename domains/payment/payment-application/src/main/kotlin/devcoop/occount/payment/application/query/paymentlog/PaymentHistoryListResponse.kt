package devcoop.occount.payment.application.query.paymentlog

import org.springframework.data.domain.Page

data class PaymentHistoryListResponse(
    val payments: List<PaymentLogResult>,
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
) {
    companion object {
        fun from(page: Page<PaymentLogResult>): PaymentHistoryListResponse {
            return PaymentHistoryListResponse(
                payments = page.content,
                totalCount = page.totalElements,
                totalPages = page.totalPages,
                currentPage = page.number,
                pageSize = page.size,
            )
        }
    }
}
