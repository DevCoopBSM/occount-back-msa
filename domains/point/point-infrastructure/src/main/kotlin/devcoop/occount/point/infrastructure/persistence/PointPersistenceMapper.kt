package devcoop.occount.point.infrastructure.persistence

import devcoop.occount.point.domain.Point

object PointPersistenceMapper {
    fun toDomain(entity: PointJpaEntity): Point {
        return Point(
            userId = entity.getUserId(),
            balance = entity.getBalance(),
        )
    }

    fun toEntity(domain: Point): PointJpaEntity {
        return PointJpaEntity(
            userId = domain.userId,
            balance = domain.balance,
        )
    }
}
