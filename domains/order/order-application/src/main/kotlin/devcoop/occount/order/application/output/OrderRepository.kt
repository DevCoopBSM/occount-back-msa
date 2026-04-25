package devcoop.occount.order.application.output

import devcoop.occount.order.domain.order.OrderAggregate
import java.time.Instant

interface OrderRepository {
    fun findById(orderId: Long): OrderAggregate?
    fun findPersistedById(orderId: Long): PersistedOrder?
    fun save(order: OrderAggregate): OrderAggregate
    fun save(order: OrderAggregate, persistenceVersion: Long): OrderAggregate
    fun findExpiredNonFinalOrderIds(now: Instant): List<Long>
}
