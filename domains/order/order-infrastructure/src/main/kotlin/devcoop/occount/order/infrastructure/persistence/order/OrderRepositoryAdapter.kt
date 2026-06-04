package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.output.PersistedOrder
import devcoop.occount.order.application.output.SalesRankingItem
import devcoop.occount.order.application.output.SalesRankingRepository
import devcoop.occount.order.application.shared.SalesRankingType
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.isFinalForClient
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class OrderRepositoryAdapter(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository, SalesRankingRepository {

    @Transactional(readOnly = true)
    override fun findById(orderId: Long): OrderAggregate? =
        orderJpaRepository.findById(orderId).orElse(null)?.let(OrderPersistenceMapper::toDomain)

    @Transactional(readOnly = true)
    override fun findPersistedById(orderId: Long): PersistedOrder? =
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

    override fun findExpiredNonFinalOrderIds(now: Instant): List<Long> {
        val finalStatuses = OrderStatus.entries.filter { it.isFinalForClient() }
        return orderJpaRepository.findExpiredNonFinalOrderIds(now, finalStatuses)
    }

    @Transactional(readOnly = true)
    override fun findOrderIdsRequiringCompensation(limit: Int): List<Long> =
        orderJpaRepository.findOrderIdsRequiringCompensation(PageRequest.ofSize(limit))

    @Transactional(readOnly = true)
    override fun findSalesRanking(
        startDateTime: Instant,
        endDateTime: Instant,
        type: SalesRankingType,
        limit: Int,
    ): List<SalesRankingItem> {
        val pageable = PageRequest.ofSize(limit)
        return when (type) {
            SalesRankingType.POPULAR -> orderJpaRepository.findPopularSalesRanking(
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                pageable = pageable,
            )

            SalesRankingType.UNPOPULAR -> orderJpaRepository.findUnpopularSalesRanking(
                startDateTime = startDateTime,
                endDateTime = endDateTime,
                pageable = pageable,
            )
        }
    }
}
