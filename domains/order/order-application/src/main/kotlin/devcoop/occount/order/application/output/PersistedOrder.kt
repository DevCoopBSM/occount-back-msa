package devcoop.occount.order.application.output

import devcoop.occount.order.domain.order.OrderAggregate

data class PersistedOrder(
    val order: OrderAggregate,
    val persistenceVersion: Long,
)
