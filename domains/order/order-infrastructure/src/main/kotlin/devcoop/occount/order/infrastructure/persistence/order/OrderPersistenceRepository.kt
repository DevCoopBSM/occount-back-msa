package devcoop.occount.order.infrastructure.persistence.order

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OrderPersistenceRepository : JpaRepository<OrderJpaEntity, String>
