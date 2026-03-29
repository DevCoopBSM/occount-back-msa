package devcoop.occount.point.application.query.balance

import devcoop.occount.point.application.exception.PointNotFoundException
import devcoop.occount.point.application.support.FakePointRepository
import devcoop.occount.point.domain.Point
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GetPointBalanceQueryServiceTest {
    @Test
    @DisplayName("잔액 조회 시 기존 포인트의 잔액을 반환한다")
    fun `getBalance returns existing point balance`() {
        val repository = FakePointRepository(
            points = mutableMapOf(1L to Point(userId = 1L, balance = 50)),
        )
        val queryService = GetPointBalanceQueryService(repository)

        val response = queryService.getBalance(1L)

        assertEquals(PointBalanceResponse(balance = 50), response)
    }

    @Test
    @DisplayName("잔액 조회 시 포인트가 없으면 PointNotFound가 발생한다")
    fun `getBalance throws PointNotFound when point does not exist`() {
        val repository = FakePointRepository()
        val queryService = GetPointBalanceQueryService(repository)

        assertFailsWith<PointNotFoundException> {
            queryService.getBalance(1L)
        }
    }
}
