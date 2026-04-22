package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.output.PersistedOrder
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.isFinalForClient
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OrderRepositoryAdapter(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {

    @Transactional(readOnly = true)
    override fun findById(orderId: String): OrderAggregate? =
        orderJpaRepository.findById(orderId).orElse(null)?.let(OrderPersistenceMapper::toDomain)

    @Transactional(readOnly = true)
    override fun findPersistedById(orderId: String): PersistedOrder? =
        orderJpaRepository.findById(orderId).orElse(null)?.let { entity ->
            PersistedOrder(
                order = OrderPersistenceMapper.toDomain(entity),
                persistenceVersion = entity.getVersion(),
            )
        }

    override fun save(order: OrderAggregate): OrderAggregate {
        val entity = OrderPersistenceMapper.toEntity(order)
        return OrderPersistenceMapper.toDomain(orderJpaRepository.save(entity))
    }

    override fun save(order: OrderAggregate, persistenceVersion: Long): OrderAggregate {
        val entity = OrderPersistenceMapper.toEntity(order, persistenceVersion)
        return OrderPersistenceMapper.toDomain(orderJpaRepository.save(entity))
    }

    override fun findExpiredNonFinalOrderIds(now: Instant): List<String> {
        val finalStatuses = OrderStatus.entries.filter { it.isFinalForClient() }
        return orderJpaRepository.findExpiredNonFinalOrderIds(now, finalStatuses)
    }
}
