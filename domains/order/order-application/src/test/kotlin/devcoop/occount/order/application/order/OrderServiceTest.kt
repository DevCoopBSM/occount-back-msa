package devcoop.occount.order.application.order

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderPaymentFailedEvent
import devcoop.occount.core.common.event.OrderPaymentType
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompletedEvent
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderLine
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderPaymentResult
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.OrderStepStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import java.time.Instant

class OrderServiceTest {
    @Test
    fun `stock success after payment failure requests stock compensation`() {
        val orderRepository = FakeOrderRepository(
            initialOrder = orderFixture(),
        )
        val eventPublisher = FakeEventPublisher()
        val orderService = OrderService(orderRepository, eventPublisher, FakeCompensationService(orderRepository, eventPublisher, TestTransactionManager()), TestTransactionManager(), 30L)

        orderService.handlePaymentFailed(
            OrderPaymentFailedEvent(
                orderId = ORDER_ID,
                userId = USER_ID,
                reason = "payment failed",
            ),
        )
        orderService.handleStockCompleted(
            OrderStockCompletedEvent(
                orderId = ORDER_ID,
                itemIds = listOf(ITEM_ID),
            ),
        )

        val publishedEvent = eventPublisher.published.last().payload
        assertInstanceOf(OrderStockCompensationRequestedEvent::class.java, publishedEvent)
        assertEquals(OrderStatus.COMPENSATING, orderRepository.findById(ORDER_ID)!!.status)
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
        val orderService = OrderService(orderRepository, eventPublisher, FakeCompensationService(orderRepository, eventPublisher, TestTransactionManager()), TestTransactionManager(), 30L)

        orderService.cancel(ORDER_ID, USER_ID)

        val publishedEvent = eventPublisher.published.last().payload
        assertInstanceOf(OrderPaymentCompensationRequestedEvent::class.java, publishedEvent)
        assertEquals(OrderStatus.CANCEL_REQUESTED, orderRepository.findById(ORDER_ID)!!.status)
    }

    private fun orderFixture(
        paymentStatus: OrderStepStatus = OrderStepStatus.PENDING,
        stockStatus: OrderStepStatus = OrderStepStatus.PENDING,
        paymentLogId: Long? = null,
        pointsUsed: Int = 0,
    ): OrderAggregate {
        return OrderAggregate(
            orderId = ORDER_ID,
            userId = USER_ID,
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
            expiresAt = Instant.now().plusSeconds(30),
        )
    }

    private class FakeCompensationService(
        private val orderRepository: OrderRepository,
        private val eventPublisher: EventPublisher,
        transactionManager: PlatformTransactionManager,
    ) : OrderCompensationService(orderRepository, eventPublisher, transactionManager)

    private class FakeOrderRepository(
        initialOrder: OrderAggregate,
    ) : OrderRepository {
        private val orders = linkedMapOf(initialOrder.orderId to initialOrder)

        override fun findById(orderId: String): OrderAggregate? = orders[orderId]

        override fun save(order: OrderAggregate): OrderAggregate {
            val persisted = order.copy(version = order.version + 1)
            orders[persisted.orderId] = persisted
            return persisted
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

    private class TestTransactionManager : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()

        override fun commit(status: TransactionStatus) = Unit

        override fun rollback(status: TransactionStatus) = Unit
    }

    private companion object {
        const val ORDER_ID = "order-1"
        const val USER_ID = 1L
        const val ITEM_ID = 101L
    }
}
