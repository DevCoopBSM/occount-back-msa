package devcoop.occount.order.application.query

import devcoop.occount.order.application.shared.SalesRankingPeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class SalesRankingDateRange(
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
