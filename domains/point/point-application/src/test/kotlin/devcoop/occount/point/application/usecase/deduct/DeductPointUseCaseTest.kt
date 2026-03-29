package devcoop.occount.point.application.usecase.deduct

import devcoop.occount.point.application.exception.PointNotFoundException
import devcoop.occount.point.application.query.balance.PointBalanceResponse
import devcoop.occount.point.application.support.FakePointRepository
import devcoop.occount.point.domain.Point
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeductPointUseCaseTest {
    @Test
    @DisplayName("차감 시 변경된 포인트를 저장하고 잔액을 반환한다")
    fun `deduct saves updated point`() {
        val repository = FakePointRepository(
            points = mutableMapOf(1L to Point(userId = 1L, balance = 100)),
        )
        val useCase = DeductPointUseCase(repository)

        val response = useCase.deduct(1L, 25)

        assertEquals(PointBalanceResponse(balance = 75), response)
        assertEquals(Point(userId = 1L, balance = 75), repository.savedPoints.single())
    }

    @Test
    @DisplayName("차감 시 포인트가 없으면 PointNotFound가 발생한다")
    fun `deduct throws PointNotFound when point does not exist`() {
        val repository = FakePointRepository()
        val useCase = DeductPointUseCase(repository)

        assertFailsWith<PointNotFoundException> {
            useCase.deduct(1L, 10)
        }
    }
}
