package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.application.order.OrderRepository
import devcoop.occount.order.domain.order.OrderAggregate
import org.springframework.stereotype.Repository

@Repository
class OrderRepositoryImpl(
    private val orderPersistenceRepository: OrderPersistenceRepository,
) : OrderRepository {
    override fun findById(orderId: String): OrderAggregate? {
        return orderPersistenceRepository.findById(orderId)
            .map(OrderPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun save(order: OrderAggregate): OrderAggregate {
        return orderPersistenceRepository.save(OrderPersistenceMapper.toEntity(order))
            .let(OrderPersistenceMapper::toDomain)
    }
}
