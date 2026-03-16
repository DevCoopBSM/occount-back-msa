package devcoop.occount.point.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PointPersistenceRepository : JpaRepository<PointJpaEntity, Long>
