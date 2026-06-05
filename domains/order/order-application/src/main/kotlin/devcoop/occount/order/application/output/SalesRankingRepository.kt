package devcoop.occount.order.application.output

import devcoop.occount.order.application.shared.SalesRankingType
import java.time.Instant

interface SalesRankingRepository {
    fun findSalesRanking(
        startDateTime: Instant,
        endDateTime: Instant,
        type: SalesRankingType,
        limit: Int,
    ): List<SalesRankingItem>
}
