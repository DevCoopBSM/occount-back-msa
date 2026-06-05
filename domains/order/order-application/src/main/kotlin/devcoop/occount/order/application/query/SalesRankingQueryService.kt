package devcoop.occount.order.application.query

import devcoop.occount.order.application.exception.OrderInvalidSalesRankingLimitException
import devcoop.occount.order.application.output.SalesRankingRepository
import devcoop.occount.order.application.shared.SalesRankingItemResponse
import devcoop.occount.order.application.shared.SalesRankingPeriod
import devcoop.occount.order.application.shared.SalesRankingResponse
import devcoop.occount.order.application.shared.SalesRankingType
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

@Service
class SalesRankingQueryService(
    private val salesRankingRepository: SalesRankingRepository,
) {
    fun getSalesRanking(
        period: SalesRankingPeriod,
        type: SalesRankingType,
        limit: Int,
        date: LocalDate = LocalDate.now(),
    ): SalesRankingResponse {
        if (limit < 1) {
            throw OrderInvalidSalesRankingLimitException()
        }

        val range = SalesRankingDateRange.from(period, date)
        val items = salesRankingRepository.findSalesRanking(
            startDateTime = range.startDate.atStartOfDay().toInstant(ZoneOffset.UTC),
            endDateTime = range.endExclusiveDate.atStartOfDay().toInstant(ZoneOffset.UTC),
            type = type,
            limit = limit,
        )

        return SalesRankingResponse(
            period = period,
            type = type,
            limit = limit,
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

private data class SalesRankingDateRange(
    val startDate: LocalDate,
    val endExclusiveDate: LocalDate,
) {
    companion object {
        fun from(period: SalesRankingPeriod, date: LocalDate): SalesRankingDateRange {
            return when (period) {
                SalesRankingPeriod.DAILY -> SalesRankingDateRange(
                    startDate = date,
                    endExclusiveDate = date.plusDays(1),
                )

                SalesRankingPeriod.WEEKLY -> {
                    val startDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    SalesRankingDateRange(
                        startDate = startDate,
                        endExclusiveDate = startDate.plusWeeks(1),
                    )
                }

                SalesRankingPeriod.MONTHLY -> {
                    val startDate = date.withDayOfMonth(1)
                    SalesRankingDateRange(
                        startDate = startDate,
                        endExclusiveDate = startDate.plusMonths(1),
                    )
                }
            }
        }
    }
}
