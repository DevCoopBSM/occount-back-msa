package devcoop.occount.point.application.usecase.charge

import devcoop.occount.point.application.exception.PointNotFound
import devcoop.occount.point.application.query.balance.PointBalanceResponse
import devcoop.occount.point.application.support.FakePointRepository
import devcoop.occount.point.domain.Point
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChargePointUseCaseTest {
    @Test
    @DisplayName("충전 시 변경된 포인트를 저장하고 잔액을 반환한다")
    fun `charge saves updated point`() {
        val repository = FakePointRepository(
            points = mutableMapOf(1L to Point(userId = 1L, balance = 30)),
        )
        val useCase = ChargePointUseCase(repository)

        val response = useCase.charge(ChargePointRequest(userId = 1L, amount = 70))

        assertEquals(PointBalanceResponse(balance = 100), response)
        assertEquals(Point(userId = 1L, balance = 100), repository.savedPoints.single())
    }

    @Test
    @DisplayName("충전 시 포인트가 없으면 PointNotFound가 발생한다")
    fun `charge throws PointNotFound when point does not exist`() {
        val repository = FakePointRepository()
        val useCase = ChargePointUseCase(repository)

        assertFailsWith<PointNotFound> {
            useCase.charge(ChargePointRequest(userId = 1L, amount = 10))
        }
    }
}
