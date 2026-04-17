package devcoop.occount.item.application.usecase.order

import devcoop.occount.core.common.event.OrderItemPayload
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.core.common.event.OrderRequestedItemPayload
import devcoop.occount.core.common.event.OrderStockCompletedEvent
import devcoop.occount.core.common.event.OrderStockFailedEvent
import devcoop.occount.item.application.support.FakeEventPublisher
import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.TestTransactionManager
import devcoop.occount.item.application.support.itemFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class ProcessOrderRequestedUseCaseTest {
    @Test
    fun `process decreases stock and publishes stock completed`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Americano", price = 2000, quantity = 10),
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val useCase = ProcessOrderRequestedUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.process(requestedEvent()) {}

        assertEquals(8, itemRepository.findById(1L)!!.getQuantity())
        val publishedEvent = assertInstanceOf(OrderStockCompletedEvent::class.java, eventPublisher.published.single())
        assertEquals(4000, publishedEvent.totalAmount)
        assertEquals(
            listOf(
                OrderItemPayload(
                    itemId = 1L,
                    itemName = "Americano",
                    itemPrice = 2000,
                    quantity = 2,
                    totalPrice = 4000,
                ),
            ),
            publishedEvent.items,
        )
    }

    @Test
    fun `process aggregates duplicated item requests before deducting stock`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Americano", price = 2000, quantity = 10),
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val useCase = ProcessOrderRequestedUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.process(
            requestedEvent(
                items = listOf(
                    OrderRequestedItemPayload(itemId = 1L, quantity = 1),
                    OrderRequestedItemPayload(itemId = 1L, quantity = 2),
                ),
            ),
        ) {}

        assertEquals(7, itemRepository.findById(1L)!!.getQuantity())
        val publishedEvent = assertInstanceOf(OrderStockCompletedEvent::class.java, eventPublisher.published.single())
        assertEquals(6000, publishedEvent.totalAmount)
        assertEquals(1, publishedEvent.items.size)
        assertEquals(3, publishedEvent.items.single().quantity)
    }

    @Test
    fun `process publishes stock failed when item is missing`() {
        val itemRepository = FakeItemRepository()
        val eventPublisher = FakeEventPublisher()
        val useCase = ProcessOrderRequestedUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.process(requestedEvent()) {}

        val publishedEvent = assertInstanceOf(OrderStockFailedEvent::class.java, eventPublisher.published.single())
        assertEquals("order-1", publishedEvent.orderId)
    }

    @Test
    fun `process retries optimistic lock failures before succeeding`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Americano", price = 2000, quantity = 10),
            ),
        ).apply {
            saveStockOptimisticLockFailuresRemaining = 1
        }
        val eventPublisher = FakeEventPublisher()
        val useCase = ProcessOrderRequestedUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.process(requestedEvent()) {}

        assertEquals(2, itemRepository.saveStocksCount)
        assertInstanceOf(OrderStockCompletedEvent::class.java, eventPublisher.published.single())
    }

    private fun requestedEvent(
        items: List<OrderRequestedItemPayload> = listOf(
            OrderRequestedItemPayload(itemId = 1L, quantity = 2),
        ),
    ): OrderRequestedEvent {
        return OrderRequestedEvent(
            orderId = "order-1",
            userId = 7L,
            kioskId = "kiosk-1",
            items = items,
        )
    }
}
