package devcoop.occount.order.api.support

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.order.application.exception.OrderConcurrencyException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.output.PersistedOrder
import devcoop.occount.order.application.output.TransactionPort
import devcoop.occount.order.application.port.OrderStatusNotifier
import devcoop.occount.order.application.shared.OrderStreamEvent
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.RequestedOrderLine
import devcoop.occount.order.domain.order.isFinalForClient
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

fun orderFixture(
    orderId: Long = 1L,
    userId: Long? = 7L,
    kioskId: String = "kiosk-1",
    requestedLines: List<RequestedOrderLine> = listOf(RequestedOrderLine(itemId = 1L, quantity = 1)),
    status: OrderStatus = OrderStatus.PROCESSING,
    expiresAt: Instant = Instant.now().plusSeconds(30),
): OrderAggregate =
    OrderAggregate.create(
        userId = userId,
        requestedLines = requestedLines,
        kioskId = kioskId,
        expiresAt = expiresAt,
    ).copy(orderId = orderId, status = status)

class FakeOrderRepository(
    initialOrders: List<OrderAggregate> = emptyList(),
) : OrderRepository {
    private val ordersById = linkedMapOf<Long, OrderAggregate>().apply {
        initialOrders.forEach { order -> put(order.orderId, order) }
    }
    private val persistenceVersions = linkedMapOf<Long, Long>().apply {
        initialOrders.forEach { order -> put(order.orderId, 0L) }
    }
    private var nextId = (ordersById.keys.maxOrNull() ?: 0L) + 1L

    override fun findById(orderId: Long): OrderAggregate? = ordersById[orderId]

    override fun findPersistedById(orderId: Long): PersistedOrder? {
        val order = ordersById[orderId] ?: return null
        return PersistedOrder(
            order = order,
            persistenceVersion = persistenceVersions.getValue(orderId),
        )
    }

    override fun save(order: OrderAggregate): OrderAggregate {
        val persistedOrder = if (order.orderId == 0L) {
            order.copy(orderId = nextId++)
        } else {
            order
        }
        ordersById[persistedOrder.orderId] = persistedOrder
        persistenceVersions[persistedOrder.orderId] = (persistenceVersions[persistedOrder.orderId] ?: -1L) + 1L
        return persistedOrder
    }

    override fun save(order: OrderAggregate, persistenceVersion: Long): OrderAggregate {
        val currentVersion = persistenceVersions[order.orderId] ?: throw OrderConcurrencyException()
        if (currentVersion != persistenceVersion) {
            throw OrderConcurrencyException()
        }
        ordersById[order.orderId] = order
        persistenceVersions[order.orderId] = currentVersion + 1L
        return order
    }

    override fun findExpiredNonFinalOrderIds(now: Instant): List<Long> {
        return ordersById.values
            .filter { order -> order.expiresAt <= now && !order.status.isFinalForClient() }
            .map(OrderAggregate::orderId)
    }

    override fun findOrderIdsRequiringCompensation(limit: Int): List<Long> {
        return ordersById.values
            .filter { order ->
                order.shouldRequestPaymentCompensation() || order.shouldRequestStockCompensation()
            }
            .map(OrderAggregate::orderId)
            .take(limit)
    }
}

class FakeTransactionPort : TransactionPort {
    override fun <T : Any> executeInNewTransaction(action: () -> T): T = action()
}

class FakeEventPublisher : EventPublisher {
    val published = mutableListOf<Any>()

    override fun publish(topic: String, key: String, eventType: String, payload: Any) {
        published += payload
    }
}

class FakeOrderStatusNotifier : OrderStatusNotifier {
    val events = mutableListOf<OrderStreamEvent>()

    override fun notify(event: OrderStreamEvent) {
        events += event
    }
}

fun mockMvc(vararg controllers: Any): MockMvc {
    val messageConverter = MappingJackson2HttpMessageConverter(
        jacksonObjectMapper(),
    )
    return MockMvcBuilders.standaloneSetup(*controllers)
        .setControllerAdvice(ApiAdviceHandler())
        .setMessageConverters(messageConverter)
        .build()
}
