package devcoop.occount.order.application.query

import devcoop.occount.order.application.shared.SalesRankingPeriod
import devcoop.occount.order.application.shared.SalesRankingType
import java.time.LocalDate

data class SalesRankingQueryCondition(
    val period: SalesRankingPeriod,
    val type: SalesRankingType,
    val limit: Int,
    val date: LocalDate,
)
