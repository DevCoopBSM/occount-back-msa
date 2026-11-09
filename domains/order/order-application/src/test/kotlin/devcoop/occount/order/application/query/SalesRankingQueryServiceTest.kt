package devcoop.occount.order.application.query

import devcoop.occount.order.application.output.SalesRankingItem
import devcoop.occount.order.application.output.SalesRankingRepository
import devcoop.occount.order.application.shared.SalesRankingPeriod
import devcoop.occount.order.application.shared.SalesRankingType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

@DisplayName("SalesRankingQueryService 테스트")
class SalesRankingQueryServiceTest {
    @Test
    @DisplayName("일간 판매 랭킹은 기준 날짜 하루 범위로 조회한다")
    fun `daily sales ranking uses one day range`() {
        val repository = FakeSalesRankingRepository()
        val service = SalesRankingQueryService(repository, SalesRankingQueryParser())

        val response = service.getSalesRanking(
            period = "DAILY",
            type = "POPULAR",
            limit = "5",
            date = "2026-06-05",
        )

        assertEquals(LocalDate.of(2026, 6, 5), response.startDate)
        assertEquals(LocalDate.of(2026, 6, 5), response.endDate)
        assertEquals(Instant.parse("2026-06-05T00:00:00Z"), repository.lastStartDateTime)
        assertEquals(Instant.parse("2026-06-06T00:00:00Z"), repository.lastEndDateTime)
        assertEquals(SalesRankingType.POPULAR, repository.lastType)
        assertEquals(5, repository.lastLimit)
    }

    @Test
    @DisplayName("주간 판매 랭킹은 기준 날짜가 포함된 월요일부터 일요일까지 조회한다")
    fun `weekly sales ranking uses monday to sunday range`() {
        val repository = FakeSalesRankingRepository()
        val service = SalesRankingQueryService(repository, SalesRankingQueryParser())

        val response = service.getSalesRanking(
            period = "WEEKLY",
            type = "UNPOPULAR",
            limit = "3",
            date = "2026-06-05",
        )

        assertEquals(LocalDate.of(2026, 6, 1), response.startDate)
        assertEquals(LocalDate.of(2026, 6, 7), response.endDate)
        assertEquals(Instant.parse("2026-06-01T00:00:00Z"), repository.lastStartDateTime)
        assertEquals(Instant.parse("2026-06-08T00:00:00Z"), repository.lastEndDateTime)
        assertEquals(SalesRankingType.UNPOPULAR, repository.lastType)
        assertEquals(3, repository.lastLimit)
    }

    @Test
    @DisplayName("월간 판매 랭킹은 기준 날짜가 포함된 월 전체 범위로 조회한다")
    fun `monthly sales ranking uses entire month range`() {
        val repository = FakeSalesRankingRepository()
        val service = SalesRankingQueryService(repository, SalesRankingQueryParser())

        val response = service.getSalesRanking(
            period = "MONTHLY",
            type = "POPULAR",
            limit = "10",
            date = "2026-02-10",
        )

        assertEquals(LocalDate.of(2026, 2, 1), response.startDate)
        assertEquals(LocalDate.of(2026, 2, 28), response.endDate)
        assertEquals(Instant.parse("2026-02-01T00:00:00Z"), repository.lastStartDateTime)
        assertEquals(Instant.parse("2026-03-01T00:00:00Z"), repository.lastEndDateTime)
    }

    private class FakeSalesRankingRepository : SalesRankingRepository {
        var lastStartDateTime: Instant? = null
        var lastEndDateTime: Instant? = null
        var lastType: SalesRankingType? = null
        var lastLimit: Int? = null

        override fun findSalesRanking(
            startDateTime: Instant,
            endDateTime: Instant,
            type: SalesRankingType,
            limit: Int,
        ): List<SalesRankingItem> {
            lastStartDateTime = startDateTime
            lastEndDateTime = endDateTime
            lastType = type
            lastLimit = limit
            return listOf(
                SalesRankingItem(
                    itemId = 1L,
                    itemName = "아메리카노",
                    soldQuantity = 12L,
                ),
            )
        }
    }
}
