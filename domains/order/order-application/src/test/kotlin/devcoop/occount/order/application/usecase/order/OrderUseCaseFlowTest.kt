package devcoop.occount.order.application.usecase.order

import devcoop.occount.core.common.event.*
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.output.PersistedOrder
import devcoop.occount.order.application.output.TransactionPort
import devcoop.occount.order.application.support.*
import devcoop.occount.order.application.usecase.order.cancel.CancelOrderUseCase
import devcoop.occount.order.application.usecase.order.event.HandleOrderPaymentEventUseCase
import devcoop.occount.order.application.usecase.order.event.HandleOrderStockEventUseCase
import devcoop.occount.order.application.usecase.order.timeout.ExpireOrderUseCase
import devcoop.occount.order.application.usecase.order.timeout.ExpireTimedOutOrdersUseCase
import devcoop.occount.order.domain.order.*
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
            PaymentFailedEvent(
                orderId = ORDER_ID,
                userId = USER_ID,
                reason = "payment failed",
            ),
            recordConsumption = {},
        )
        handleOrderStockEventUseCase.applyCompletedStock(
            completedStockEvent(),
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
            completedStockEvent(),
            recordConsumption = {},
        )

        val publishedEvent = eventPublisher.published.single().payload
        assertInstanceOf(OrderPaymentRequestedEvent::class.java, publishedEvent)
        assertEquals(true, orderRepository.findById(ORDER_ID)!!.paymentRequested)
    }

    @Test
    fun `stock failure without payment request ends order as failed`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(),
        )
        val eventPublisher = FakeEventPublisher()
        val handleOrderStockEventUseCase = handleOrderStockEventUseCase(orderRepository, eventPublisher)

        handleOrderStockEventUseCase.applyFailedStock(
            ItemStockDecreaseFailedEvent(
                orderId = ORDER_ID,
                reason = "out of stock",
            ),
            recordConsumption = {},
        )

        val order = orderRepository.findById(ORDER_ID)!!
        assertEquals(OrderStepStatus.FAILED, order.stockStatus)
        assertEquals(OrderStatus.FAILED, order.status)
        assertEquals(0, eventPublisher.published.size)
    }

    @Test
    fun `duplicate stock success does not request payment twice`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(),
        )
        val eventPublisher = FakeEventPublisher()
        val handleOrderStockEventUseCase = handleOrderStockEventUseCase(orderRepository, eventPublisher)

        handleOrderStockEventUseCase.applyCompletedStock(
            completedStockEvent(),
            recordConsumption = {},
        )
        handleOrderStockEventUseCase.applyCompletedStock(
            completedStockEvent(),
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
    fun `cancel requests pending payment cancellation when payment is in progress`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(
                paymentStatus = OrderStepStatus.PENDING,
                stockStatus = OrderStepStatus.SUCCEEDED,
                paymentRequested = true,
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val cancelOrderUseCase = cancelOrderUseCase(orderRepository, eventPublisher)

        cancelOrderUseCase.cancel(ORDER_ID, KIOSK_ID)

        assertEquals(1, eventPublisher.published.filter { it.payload is OrderPaymentCancellationRequestedEvent }.size)
        assertEquals(true, orderRepository.findById(ORDER_ID)!!.paymentCancellationRequested)
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
            PaymentCompletedEvent(
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
            PaymentCompletedEvent(
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

    @Test
    fun `payment failure truncates failure reason to fit order column`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(),
        )
        val eventPublisher = FakeEventPublisher()
        val handleOrderPaymentEventUseCase = handleOrderPaymentEventUseCase(orderRepository, eventPublisher)

        handleOrderPaymentEventUseCase.applyFailedPayment(
            PaymentFailedEvent(
                orderId = ORDER_ID,
                userId = USER_ID,
                reason = "x".repeat(400),
            ),
            recordConsumption = {},
        )

        assertEquals(255, orderRepository.findById(ORDER_ID)!!.failureReason!!.length)
    }

    private fun orderFixture(
        paymentStatus: OrderStepStatus = OrderStepStatus.PENDING,
        stockStatus: OrderStepStatus = OrderStepStatus.PENDING,
        paymentLogId: Long? = null,
        pointsUsed: Int = 0,
        expiresAt: Instant = Instant.now().plusSeconds(30),
        paymentRequested: Boolean = false,
    ): OrderAggregate {
        return OrderAggregate(
            orderId = ORDER_ID,
            userId = USER_ID,
            kioskId = KIOSK_ID,
            requestedLines = listOf(
                RequestedOrderLine(
                    itemId = ITEM_ID,
                    quantity = 1,
                ),
            ),
            lines = if (stockStatus == OrderStepStatus.SUCCEEDED) {
                listOf(
                    OrderLine(
                        itemId = ITEM_ID,
                        itemNameSnapshot = "Americano",
                        unitPrice = 2000,
                        quantity = 1,
                        totalPrice = 2000,
                    ),
                )
            } else {
                emptyList()
            },
            payment = OrderPayment(
                totalAmount = if (stockStatus == OrderStepStatus.SUCCEEDED) 2000 else 0,
            ),
            status = OrderStatus.PROCESSING,
            paymentStatus = paymentStatus,
            stockStatus = stockStatus,
            paymentResult = OrderPaymentResult(
                paymentLogId = paymentLogId,
                pointsUsed = pointsUsed,
            ),
            expiresAt = expiresAt,
            paymentRequested = paymentRequested,
        )
    }

    private fun completedStockEvent(): ItemStockDecreasedEvent {
        return ItemStockDecreasedEvent(
            orderId = ORDER_ID,
            items = listOf(
                ItemStockPayload(
                    itemId = ITEM_ID,
                    itemName = "Americano",
                    itemPrice = 2000,
                    quantity = 1,
                    totalPrice = 2000,
                ),
            ),
            totalAmount = 2000,
        )
    }

    private fun cancelOrderUseCase(
        orderRepository: FakeOrderRepository,
        eventPublisher: FakeEventPublisher,
    ): CancelOrderUseCase {
        return CancelOrderUseCase(
            orderMutationExecutor = OrderMutationExecutor(orderRepository, TestTransactionPort()),
            orderLifecycleProcessor = orderLifecycleProcessor(orderRepository, eventPublisher),
            orderPaymentCancellationEventPublisher = OrderPaymentCancellationEventPublisher(eventPublisher),
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
                NoOpOrderStatusNotifier(),
                OrderStreamEventMapper(OrderResponseMapper()),
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
                orderPaymentCancellationEventPublisher = OrderPaymentCancellationEventPublisher(eventPublisher),
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
            orderStatusNotifier = NoOpOrderStatusNotifier(),
            orderStreamEventMapper = OrderStreamEventMapper(OrderResponseMapper()),
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

    private class NoOpOrderStatusNotifier : devcoop.occount.order.application.port.OrderStatusNotifier {
        override fun notify(event: devcoop.occount.order.application.shared.OrderStreamEvent) = Unit
    }

    private companion object {
        const val ORDER_ID = "order-1"
        const val USER_ID = 1L
        const val ITEM_ID = 101L
        const val KIOSK_ID = "kiosk-1"
    }
}
