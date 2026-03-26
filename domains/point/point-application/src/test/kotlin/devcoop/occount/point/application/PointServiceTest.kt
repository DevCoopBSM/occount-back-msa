package devcoop.occount.point.application

import devcoop.occount.point.application.exception.PointAlreadyInitializedException
import devcoop.occount.point.application.exception.PointNotFound
import devcoop.occount.point.application.output.PointRepository
import devcoop.occount.point.domain.Point
import org.junit.jupiter.api.DisplayName
import org.springframework.dao.DuplicateKeyException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PointServiceTest {
    @Test
    @DisplayName("포인트 초기화 시 새 포인트를 저장한다")
    fun `initialize saves new point`() {
        val repository = FakePointRepository()
        val service = PointService(repository)

        service.initialize(1L)

        assertEquals(1, repository.savedPoints.size)
        assertEquals(Point(userId = 1L, balance = 0), repository.savedPoints.single())
    }

    @Test
    @DisplayName("포인트 초기화 시 중복 키 예외는 PointAlreadyInitializedException으로 변환된다")
    fun `initialize converts duplicate key exception to PointAlreadyInitializedException`() {
        val repository = FakePointRepository(
            saveException = DuplicateKeyException("duplicate"),
        )
        val service = PointService(repository)

        assertFailsWith<PointAlreadyInitializedException> {
            service.initialize(1L)
        }
    }

    @Test
    @DisplayName("잔액 조회 시 기존 포인트의 잔액을 반환한다")
    fun `getBalance returns existing point balance`() {
        val repository = FakePointRepository(
            points = mutableMapOf(1L to Point(userId = 1L, balance = 50)),
        )
        val service = PointService(repository)

        val response = service.getBalance(1L)

        assertEquals(50, response.balance)
    }

    @Test
    @DisplayName("잔액 조회 시 포인트가 없으면 PointNotFound가 발생한다")
    fun `getBalance throws PointNotFound when point does not exist`() {
        val repository = FakePointRepository()
        val service = PointService(repository)

        assertFailsWith<PointNotFound> {
            service.getBalance(1L)
        }
    }

    @Test
    @DisplayName("충전 시 변경된 포인트를 저장한다")
    fun `charge saves updated point`() {
        val repository = FakePointRepository(
            points = mutableMapOf(1L to Point(userId = 1L, balance = 30)),
        )
        val service = PointService(repository)

        val response = service.charge(1L, 70)

        assertEquals(100, response.balance)
        assertEquals(1, repository.savedPoints.size)
        assertEquals(Point(userId = 1L, balance = 100), repository.savedPoints.single())
    }

    @Test
    @DisplayName("차감 시 변경된 포인트를 저장한다")
    fun `deduct saves updated point`() {
        val repository = FakePointRepository(
            points = mutableMapOf(1L to Point(userId = 1L, balance = 100)),
        )
        val service = PointService(repository)

        val response = service.deduct(1L, 25)

        assertEquals(75, response.balance)
        assertEquals(Point(userId = 1L, balance = 75), repository.savedPoints.single())
    }

    @Test
    @DisplayName("충전 시 포인트가 없으면 PointNotFound가 발생한다")
    fun `charge throws PointNotFound when point does not exist`() {
        val repository = FakePointRepository()
        val service = PointService(repository)

        assertFailsWith<PointNotFound> {
            service.charge(1L, 10)
        }
    }

    @Test
    @DisplayName("차감 시 포인트가 없으면 PointNotFound가 발생한다")
    fun `deduct throws PointNotFound when point does not exist`() {
        val repository = FakePointRepository()
        val service = PointService(repository)

        assertFailsWith<PointNotFound> {
            service.deduct(1L, 10)
        }
    }

    private class FakePointRepository(
        private val points: MutableMap<Long, Point> = mutableMapOf(),
        private val findException: RuntimeException? = null,
        private val saveException: RuntimeException? = null,
    ) : PointRepository {
        val savedPoints = mutableListOf<Point>()

        override fun findByUserId(userId: Long): Point? {
            findException?.let { throw it }
            return points[userId]
        }

        override fun save(point: Point): Point {
            saveException?.let { throw it }
            points[point.userId] = point
            savedPoints += point
            return point
        }
    }
}
