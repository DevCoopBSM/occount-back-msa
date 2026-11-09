package devcoop.occount.order.application.query

import devcoop.occount.order.application.exception.OrderInvalidSalesRankingDateException
import devcoop.occount.order.application.exception.OrderInvalidSalesRankingLimitException
import devcoop.occount.order.application.exception.OrderInvalidSalesRankingPeriodException
import devcoop.occount.order.application.exception.OrderInvalidSalesRankingTypeException
import devcoop.occount.order.application.shared.SalesRankingPeriod
import devcoop.occount.order.application.shared.SalesRankingType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@DisplayName("SalesRankingQueryParser 테스트")
class SalesRankingQueryParserTest {
    private val parser = SalesRankingQueryParser()

    @Test
    @DisplayName("판매량 랭킹 조회 파라미터를 조회 조건으로 변환한다")
    fun `parse sales ranking query condition`() {
        val condition = parser.parse(
            period = "DAILY",
            type = "POPULAR",
            limit = "5",
            date = "2026-06-05",
        )

        assertEquals(SalesRankingPeriod.DAILY, condition.period)
        assertEquals(SalesRankingType.POPULAR, condition.type)
        assertEquals(5, condition.limit)
        assertEquals(LocalDate.of(2026, 6, 5), condition.date)
    }

    @Test
    @DisplayName("조회 기간이 올바르지 않으면 판매량 랭킹 조회 기간 예외를 발생시킨다")
    fun `parse throws exception when period is invalid`() {
        assertThrows<OrderInvalidSalesRankingPeriodException> {
            parser.parse(
                period = "INVALID",
                type = "POPULAR",
                limit = "5",
                date = "2026-06-05",
            )
        }
    }

    @Test
    @DisplayName("조회 유형이 올바르지 않으면 판매량 랭킹 조회 유형 예외를 발생시킨다")
    fun `parse throws exception when type is invalid`() {
        assertThrows<OrderInvalidSalesRankingTypeException> {
            parser.parse(
                period = "DAILY",
                type = "INVALID",
                limit = "5",
                date = "2026-06-05",
            )
        }
    }

    @Test
    @DisplayName("조회 개수가 숫자가 아니면 판매량 랭킹 조회 개수 예외를 발생시킨다")
    fun `parse throws exception when limit is not number`() {
        assertThrows<OrderInvalidSalesRankingLimitException> {
            parser.parse(
                period = "DAILY",
                type = "POPULAR",
                limit = "INVALID",
                date = "2026-06-05",
            )
        }
    }

    @Test
    @DisplayName("조회 개수가 1 미만이면 판매량 랭킹 조회 개수 예외를 발생시킨다")
    fun `parse throws exception when limit is less than one`() {
        assertThrows<OrderInvalidSalesRankingLimitException> {
            parser.parse(
                period = "DAILY",
                type = "POPULAR",
                limit = "0",
                date = "2026-06-05",
            )
        }
    }

    @Test
    @DisplayName("조회 기준일이 올바르지 않으면 판매량 랭킹 조회 기준일 예외를 발생시킨다")
    fun `parse throws exception when date is invalid`() {
        assertThrows<OrderInvalidSalesRankingDateException> {
            parser.parse(
                period = "DAILY",
                type = "POPULAR",
                limit = "5",
                date = "INVALID",
            )
        }
    }
}
