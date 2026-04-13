package devcoop.occount.order.application.output

import devcoop.occount.order.domain.order.OrderAggregate
import java.time.Instant

interface OrderRepository {
    fun findById(orderId: String): OrderAggregate?

    fun save(order: OrderAggregate): OrderAggregate

    fun findPersistedById(orderId: String): PersistedOrder?

    fun save(order: OrderAggregate, persistenceVersion: Long): OrderAggregate

    fun findExpiredNonFinalOrderIds(now: Instant): List<String>
}
