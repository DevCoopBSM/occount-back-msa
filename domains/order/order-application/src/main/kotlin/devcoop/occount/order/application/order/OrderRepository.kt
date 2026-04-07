package devcoop.occount.order.application.order

import devcoop.occount.order.domain.order.OrderAggregate

interface OrderRepository {
    fun findById(orderId: String): OrderAggregate?

    fun save(order: OrderAggregate): OrderAggregate
}
