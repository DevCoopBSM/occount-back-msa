package devcoop.occount.order.application.usecase.order

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderPaymentCompletedEvent
import devcoop.occount.core.common.event.OrderPaymentFailedEvent
import devcoop.occount.core.common.event.OrderPaymentRequestedEvent
import devcoop.occount.core.common.event.OrderPaymentType
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompletedEvent
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.output.PersistedOrder
import devcoop.occount.order.application.output.TransactionPort
import devcoop.occount.order.application.support.OrderCompensationScheduler
import devcoop.occount.order.application.support.OrderLifecycleProcessor
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.application.support.OrderPaymentRequestScheduler
import devcoop.occount.order.application.support.OrderRequestValidator
import devcoop.occount.order.application.support.OrderResponseMapper
import devcoop.occount.order.application.usecase.order.cancel.CancelOrderUseCase
import devcoop.occount.order.application.usecase.order.create.ExpireOrderUseCase
import devcoop.occount.order.application.usecase.order.event.HandleOrderPaymentEventUseCase
import devcoop.occount.order.application.usecase.order.event.HandleOrderStockEventUseCase
import devcoop.occount.order.application.usecase.order.timeout.ExpireTimedOutOrdersUseCase
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderLine
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderPaymentResult
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.OrderStepStatus
import devcoop.occount.order.domain.order.isFinalForClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.time.Instant

class OrderUseCaseFlowTest {
    @Test
    fun `stock success after payment failure requests stock compensation`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(),
        )
        val eventPublisher = FakeEventPublisher()
        val handleOrderPaymentEventUseCase = handleOrderPaymentEventUseCase(orderRepository, eventPublisher)
        val handleOrderStockEventUseCase = handleOrderStockEventUseCase(orderRepository, eventPublisher)

        handleOrderPaymentEventUseCase.applyFailedPayment(
            OrderPaymentFailedEvent(
                orderId = ORDER_ID,
                userId = USER_ID,
                reason = "payment failed",
            ),
            recordConsumption = {},
        )
        handleOrderStockEventUseCase.applyCompletedStock(
            OrderStockCompletedEvent(
                orderId = ORDER_ID,
                itemIds = listOf(ITEM_ID),
            ),
            recordConsumption = {},
        )

        val publishedEvent = eventPublisher.published.last().payload
        assertInstanceOf(OrderStockCompensationRequestedEvent::class.java, publishedEvent)
        assertEquals(OrderStatus.COMPENSATING, orderRepository.findById(ORDER_ID)!!.status)
    }

    @Test
    fun `stock success requests payment after stock is secured`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(),
        )
        val eventPublisher = FakeEventPublisher()
        val handleOrderStockEventUseCase = handleOrderStockEventUseCase(orderRepository, eventPublisher)

        handleOrderStockEventUseCase.applyCompletedStock(
            OrderStockCompletedEvent(
                orderId = ORDER_ID,
                itemIds = listOf(ITEM_ID),
            ),
            recordConsumption = {},
        )

        val publishedEvent = eventPublisher.published.single().payload
        assertInstanceOf(OrderPaymentRequestedEvent::class.java, publishedEvent)
        assertEquals(true, orderRepository.findById(ORDER_ID)!!.paymentRequested)
    }

    @Test
    fun `duplicate stock success does not request payment twice`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(),
        )
        val eventPublisher = FakeEventPublisher()
        val handleOrderStockEventUseCase = handleOrderStockEventUseCase(orderRepository, eventPublisher)

        handleOrderStockEventUseCase.applyCompletedStock(
            OrderStockCompletedEvent(
                orderId = ORDER_ID,
                itemIds = listOf(ITEM_ID),
            ),
            recordConsumption = {},
        )
        handleOrderStockEventUseCase.applyCompletedStock(
            OrderStockCompletedEvent(
                orderId = ORDER_ID,
                itemIds = listOf(ITEM_ID),
            ),
            recordConsumption = {},
        )

        assertEquals(1, eventPublisher.published.filter { it.payload is OrderPaymentRequestedEvent }.size)
    }

    @Test
    fun `cancel requests payment compensation for succeeded payment`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(
                paymentStatus = OrderStepStatus.SUCCEEDED,
                paymentLogId = 10L,
                pointsUsed = 1000,
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val cancelOrderUseCase = cancelOrderUseCase(orderRepository, eventPublisher)

        cancelOrderUseCase.cancel(ORDER_ID, KIOSK_ID)

        val publishedEvent = eventPublisher.published.last().payload
        assertInstanceOf(OrderPaymentCompensationRequestedEvent::class.java, publishedEvent)
        assertEquals(OrderStatus.CANCEL_REQUESTED, orderRepository.findById(ORDER_ID)!!.status)
    }

    @Test
    fun `expired order is timed out and compensation is requested only once`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(
                paymentStatus = OrderStepStatus.SUCCEEDED,
                paymentLogId = 10L,
                pointsUsed = 1000,
                expiresAt = Instant.now().minusSeconds(5),
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val expireTimedOutOrdersUseCase = expireTimedOutOrdersUseCase(orderRepository, eventPublisher)

        expireTimedOutOrdersUseCase.expire()
        expireTimedOutOrdersUseCase.expire()

        val publishedEvents = eventPublisher.published.map { it.payload }
        assertEquals(1, publishedEvents.filterIsInstance<OrderPaymentCompensationRequestedEvent>().size)
        assertEquals(OrderStatus.TIMED_OUT, orderRepository.findById(ORDER_ID)!!.status)
    }

    @Test
    fun `duplicate payment completion does not save unchanged order`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(
                paymentStatus = OrderStepStatus.SUCCEEDED,
                paymentLogId = 10L,
                pointsUsed = 1000,
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val handleOrderPaymentEventUseCase = handleOrderPaymentEventUseCase(orderRepository, eventPublisher)

        handleOrderPaymentEventUseCase.applyCompletedPayment(
            OrderPaymentCompletedEvent(
                orderId = ORDER_ID,
                userId = USER_ID,
                paymentLogId = 10L,
                pointsUsed = 1000,
                cardAmount = 1000,
                totalAmount = 2000,
                transactionId = "tx-1",
                approvalNumber = "ap-1",
            ),
            recordConsumption = {},
        )

        assertEquals(0, orderRepository.versionedSaveCount)
        assertEquals(0, eventPublisher.published.size)
    }

    @Test
    fun `payment completion does not trigger compensation lookup while order is still processing`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(),
        )
        val eventPublisher = FakeEventPublisher()
        val handleOrderPaymentEventUseCase = handleOrderPaymentEventUseCase(orderRepository, eventPublisher)

        handleOrderPaymentEventUseCase.applyCompletedPayment(
            OrderPaymentCompletedEvent(
                orderId = ORDER_ID,
                userId = USER_ID,
                paymentLogId = 10L,
                pointsUsed = 1000,
                cardAmount = 1000,
                totalAmount = 2000,
                transactionId = "tx-1",
                approvalNumber = "ap-1",
            ),
            recordConsumption = {},
        )

        assertEquals(1, orderRepository.persistedLookupCount)
        assertEquals(1, orderRepository.versionedSaveCount)
        assertEquals(0, eventPublisher.published.size)
    }

    private fun orderFixture(
        paymentStatus: OrderStepStatus = OrderStepStatus.PENDING,
        stockStatus: OrderStepStatus = OrderStepStatus.PENDING,
        paymentLogId: Long? = null,
        pointsUsed: Int = 0,
        expiresAt: Instant = Instant.now().plusSeconds(30),
    ): OrderAggregate {
        return OrderAggregate(
            orderId = ORDER_ID,
            userId = USER_ID,
            kioskId = KIOSK_ID,
            lines = listOf(
                OrderLine(
                    itemId = ITEM_ID,
                    itemNameSnapshot = "Americano",
                    unitPrice = 2000,
                    quantity = 1,
                    totalPrice = 2000,
                ),
            ),
            payment = OrderPayment(
                type = OrderPaymentType.PAYMENT,
                totalAmount = 2000,
            ),
            status = OrderStatus.PROCESSING,
            paymentStatus = paymentStatus,
            stockStatus = stockStatus,
            paymentResult = OrderPaymentResult(
                paymentLogId = paymentLogId,
                pointsUsed = pointsUsed,
            ),
            expiresAt = expiresAt,
        )
    }

    private fun cancelOrderUseCase(
        orderRepository: FakeOrderRepository,
        eventPublisher: FakeEventPublisher,
    ): CancelOrderUseCase {
        return CancelOrderUseCase(
            orderMutationExecutor = OrderMutationExecutor(orderRepository, TestTransactionPort()),
            orderRepository = orderRepository,
            orderLifecycleProcessor = orderLifecycleProcessor(orderRepository, eventPublisher),
            orderResponseMapper = OrderResponseMapper(),
        )
    }

    private fun handleOrderPaymentEventUseCase(
        orderRepository: FakeOrderRepository,
        eventPublisher: FakeEventPublisher,
    ): HandleOrderPaymentEventUseCase {
        return HandleOrderPaymentEventUseCase(
            orderMutationExecutor = OrderMutationExecutor(orderRepository, TestTransactionPort()),
            orderLifecycleProcessor = orderLifecycleProcessor(orderRepository, eventPublisher),
        )
    }

    private fun handleOrderStockEventUseCase(
        orderRepository: FakeOrderRepository,
        eventPublisher: FakeEventPublisher,
    ): HandleOrderStockEventUseCase {
        return HandleOrderStockEventUseCase(
            orderMutationExecutor = OrderMutationExecutor(orderRepository, TestTransactionPort()),
            orderLifecycleProcessor = orderLifecycleProcessor(orderRepository, eventPublisher),
            orderPaymentRequestScheduler = OrderPaymentRequestScheduler(
                orderRepository,
                eventPublisher,
                TestTransactionPort(),
            ),
        )
    }

    private fun expireTimedOutOrdersUseCase(
        orderRepository: FakeOrderRepository,
        eventPublisher: FakeEventPublisher,
    ): ExpireTimedOutOrdersUseCase {
        return ExpireTimedOutOrdersUseCase(
            orderRepository = orderRepository,
            expireOrderUseCase = ExpireOrderUseCase(
                orderMutationExecutor = OrderMutationExecutor(orderRepository, TestTransactionPort()),
                orderLifecycleProcessor = orderLifecycleProcessor(orderRepository, eventPublisher),
                orderResponseMapper = OrderResponseMapper(),
            ),
        )
    }

    private fun orderLifecycleProcessor(
        orderRepository: FakeOrderRepository,
        eventPublisher: FakeEventPublisher,
    ): OrderLifecycleProcessor {
        return OrderLifecycleProcessor(
            orderCompensationScheduler = OrderCompensationScheduler(orderRepository, eventPublisher, TestTransactionPort()),
        )
    }

    private class FakeOrderRepository(
        initialOrder: OrderAggregate,
    ) : OrderRepository {
        private val orders = linkedMapOf(initialOrder.orderId to initialOrder)
        var persistedLookupCount = 0
            private set
        var versionedSaveCount = 0
            private set

        override fun findById(orderId: String): OrderAggregate? = orders[orderId]

        override fun findPersistedById(orderId: String): PersistedOrder? {
            persistedLookupCount += 1
            return orders[orderId]?.let { order ->
                PersistedOrder(
                    order = order,
                    persistenceVersion = 0L,
                )
            }
        }

        override fun save(order: OrderAggregate): OrderAggregate {
            orders[order.orderId] = order
            return order
        }

        override fun save(order: OrderAggregate, persistenceVersion: Long): OrderAggregate {
            versionedSaveCount += 1
            orders[order.orderId] = order
            return order
        }

        override fun findExpiredNonFinalOrderIds(now: Instant): List<String> {
            return orders.values
                .filter { it.expiresAt <= now && !it.status.isFinalForClient() }
                .map { it.orderId }
        }
    }

    private class FakeEventPublisher : EventPublisher {
        val published = mutableListOf<PublishedEvent>()

        override fun publish(topic: String, key: String, eventType: String, payload: Any) {
            published += PublishedEvent(topic, key, eventType, payload)
        }
    }

    private data class PublishedEvent(
        val topic: String,
        val key: String,
        val eventType: String,
        val payload: Any,
    )

    private class TestTransactionPort : TransactionPort {
        override fun <T : Any> executeInNewTransaction(action: () -> T): T = action()
    }

    private companion object {
        const val ORDER_ID = "order-1"
        const val USER_ID = 1L
        const val ITEM_ID = 101L
        const val KIOSK_ID = "kiosk-1"
    }
}
