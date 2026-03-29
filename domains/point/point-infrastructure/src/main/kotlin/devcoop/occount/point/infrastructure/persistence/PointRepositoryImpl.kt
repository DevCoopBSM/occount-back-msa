package devcoop.occount.point.infrastructure.persistence

import devcoop.occount.point.application.output.PointRepository
import devcoop.occount.point.domain.Point
import org.springframework.stereotype.Repository

@Repository
class PointRepositoryImpl(
    private val pointPersistenceRepository: PointPersistenceRepository,
) : PointRepository {
    override fun findByUserId(userId: Long): Point? {
        return pointPersistenceRepository.findById(userId)
            .map(PointPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun save(point: Point): Point {
        return pointPersistenceRepository.save(PointPersistenceMapper.toEntity(point))
            .let(PointPersistenceMapper::toDomain)
    }
}
