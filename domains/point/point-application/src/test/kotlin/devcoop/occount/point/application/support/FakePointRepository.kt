package devcoop.occount.point.application.support

import devcoop.occount.point.application.output.PointRepository
import devcoop.occount.point.domain.Point

class FakePointRepository(
    private val points: MutableMap<Long, Point> = mutableMapOf(),
    private val saveException: RuntimeException? = null,
) : PointRepository {
    val savedPoints = mutableListOf<Point>()

    override fun findByUserId(userId: Long): Point? = points[userId]

    override fun save(point: Point): Point {
        saveException?.let { throw it }
        points[point.userId] = point
        savedPoints += point
        return point
    }
}
