package devcoop.occount.point.infrastructure.persistence

import devcoop.occount.point.domain.Point

object PointPersistenceMapper {
    fun toDomain(entity: PointJpaEntity): Point {
        return Point(
            userId = entity.getUserId(),
            balance = entity.getBalance(),
            version = entity.getVersion(),
        )
    }

    fun toEntity(domain: Point): PointJpaEntity {
        return PointJpaEntity(
            userId = domain.userId,
            balance = domain.balance,
            version = domain.version,
        )
    }
}
