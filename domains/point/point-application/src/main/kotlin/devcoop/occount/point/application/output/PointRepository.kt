package devcoop.occount.point.application.output

import devcoop.occount.point.domain.Point

interface PointRepository {
    fun findByUserId(userId: Long): Point?

    fun save(point: Point): Point
}
