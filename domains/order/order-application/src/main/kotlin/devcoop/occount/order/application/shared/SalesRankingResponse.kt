package devcoop.occount.order.application.shared

import java.time.LocalDate

data class SalesRankingResponse(
    val period: SalesRankingPeriod,
    val type: SalesRankingType,
    val limit: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val items: List<SalesRankingItemResponse>,
)

data class SalesRankingItemResponse(
    val itemId: Long,
    val itemName: String,
    val soldQuantity: Long,
)
