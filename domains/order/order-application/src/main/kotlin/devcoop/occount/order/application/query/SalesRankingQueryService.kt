package devcoop.occount.order.application.query

import devcoop.occount.order.application.output.SalesRankingRepository
import devcoop.occount.order.application.shared.SalesRankingItemResponse
import devcoop.occount.order.application.shared.SalesRankingResponse
import org.springframework.stereotype.Service
import java.time.ZoneOffset

@Service
class SalesRankingQueryService(
    private val salesRankingRepository: SalesRankingRepository,
    private val salesRankingQueryParser: SalesRankingQueryParser,
) {
    fun getSalesRanking(
        period: String?,
        type: String?,
        limit: String,
        date: String?,
    ): SalesRankingResponse {
        val condition = salesRankingQueryParser.parse(
            period = period,
            type = type,
            limit = limit,
            date = date,
        )
        val range = SalesRankingDateRange.from(condition.period, condition.date)
        val items = salesRankingRepository.findSalesRanking(
            startDateTime = range.startDate.atStartOfDay().toInstant(ZoneOffset.UTC),
            endDateTime = range.endExclusiveDate.atStartOfDay().toInstant(ZoneOffset.UTC),
            type = condition.type,
            limit = condition.limit,
        )

        return SalesRankingResponse(
            period = condition.period,
            type = condition.type,
            limit = condition.limit,
            startDate = range.startDate,
            endDate = range.endExclusiveDate.minusDays(1),
            items = items.map {
                SalesRankingItemResponse(
                    itemId = it.itemId,
                    itemName = it.itemName,
                    soldQuantity = it.soldQuantity,
                )
            },
        )
    }
}
