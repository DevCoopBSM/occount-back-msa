package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.application.output.PersistedOrder
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.isFinalForClient
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class OrderRepositoryImpl(
    private val orderPersistenceRepository: OrderPersistenceRepository,
) : OrderRepository {
    override fun findById(orderId: String): OrderAggregate? {
        return orderPersistenceRepository.findById(orderId)
            .map(OrderPersistenceMapper::toDomain)
            .orElse(null)
    }

    override fun findPersistedById(orderId: String): PersistedOrder? {
        return orderPersistenceRepository.findById(orderId)
            .map { entity ->
                PersistedOrder(
                    order = OrderPersistenceMapper.toDomain(entity),
                    persistenceVersion = entity.getVersion(),
                )
            }
            .orElse(null)
    }

    override fun save(order: OrderAggregate): OrderAggregate {
        return orderPersistenceRepository.save(OrderPersistenceMapper.toEntity(order))
            .let(OrderPersistenceMapper::toDomain)
    }

    override fun save(order: OrderAggregate, persistenceVersion: Long): OrderAggregate {
        return orderPersistenceRepository.save(
            OrderPersistenceMapper.toEntity(order, persistenceVersion),
        ).let(OrderPersistenceMapper::toDomain)
    }

    override fun findExpiredNonFinalOrderIds(now: Instant): List<String> {
        return orderPersistenceRepository.findExpiredOrderIds(now, FINAL_STATUSES)
    }

    companion object {
        private val FINAL_STATUSES = OrderStatus.entries.filter { it.isFinalForClient() }
    }
}
