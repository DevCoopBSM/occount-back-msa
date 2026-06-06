package devcoop.occount.order.application.query

import devcoop.occount.order.application.exception.OrderInvalidSalesRankingDateException
import devcoop.occount.order.application.exception.OrderInvalidSalesRankingLimitException
import devcoop.occount.order.application.exception.OrderInvalidSalesRankingPeriodException
import devcoop.occount.order.application.exception.OrderInvalidSalesRankingTypeException
import devcoop.occount.order.application.shared.SalesRankingPeriod
import devcoop.occount.order.application.shared.SalesRankingType
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeParseException

@Component
class SalesRankingQueryParser {
    fun parse(
        period: String?,
        type: String?,
        limit: String,
        date: String?,
    ): SalesRankingQueryCondition {
        return SalesRankingQueryCondition(
            period = parsePeriod(period),
            type = parseType(type),
            limit = parseLimit(limit),
            date = parseDate(date),
        )
    }

    private fun parsePeriod(period: String?): SalesRankingPeriod {
        return period?.let {
            runCatching { SalesRankingPeriod.valueOf(it) }.getOrNull()
        } ?: throw OrderInvalidSalesRankingPeriodException()
    }

    private fun parseType(type: String?): SalesRankingType {
        return type?.let {
            runCatching { SalesRankingType.valueOf(it) }.getOrNull()
        } ?: throw OrderInvalidSalesRankingTypeException()
    }

    private fun parseLimit(limit: String): Int {
        val parsedLimit = limit.toIntOrNull() ?: throw OrderInvalidSalesRankingLimitException()
        if (parsedLimit < 1) {
            throw OrderInvalidSalesRankingLimitException()
        }
        return parsedLimit
    }

    private fun parseDate(date: String?): LocalDate {
        if (date == null) {
            return LocalDate.now()
        }

        try {
            return LocalDate.parse(date)
        } catch (e: DateTimeParseException) {
            throw OrderInvalidSalesRankingDateException()
        }
    }
}
