package devcoop.occount.point.application.usecase.initialize

import devcoop.occount.point.application.exception.PointAlreadyInitializedException
import devcoop.occount.point.application.support.FakePointRepository
import devcoop.occount.point.domain.Point
import org.junit.jupiter.api.DisplayName
import org.springframework.dao.DuplicateKeyException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InitializePointUseCaseTest {
    @Test
    @DisplayName("포인트 초기화 시 새 포인트를 저장한다")
    fun `initialize saves new point`() {
        val repository = FakePointRepository()
        val useCase = InitializePointUseCase(repository)

        useCase.initialize(1L)

        assertEquals(1, repository.savedPoints.size)
        assertEquals(Point(userId = 1L, balance = 0), repository.savedPoints.single())
    }

    @Test
    @DisplayName("포인트 초기화 시 중복 키 예외는 PointAlreadyInitializedException으로 변환된다")
    fun `initialize converts duplicate key exception to PointAlreadyInitializedException`() {
        val repository = FakePointRepository(
            saveException = DuplicateKeyException("duplicate"),
        )
        val useCase = InitializePointUseCase(repository)

        assertFailsWith<PointAlreadyInitializedException> {
            useCase.initialize(1L)
        }
    }
}
