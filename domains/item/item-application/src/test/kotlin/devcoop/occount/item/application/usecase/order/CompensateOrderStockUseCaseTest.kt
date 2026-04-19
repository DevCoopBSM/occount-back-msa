package devcoop.occount.item.application.usecase.order

import devcoop.occount.core.common.event.OrderStockCompensatedEvent
import devcoop.occount.core.common.event.OrderStockCompensationFailedEvent
import devcoop.occount.core.common.event.OrderStockCompensationItemPayload
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
import devcoop.occount.item.application.support.FakeEventPublisher
import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.TestTransactionManager
import devcoop.occount.item.application.support.itemFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class CompensateOrderStockUseCaseTest {
    @Test
    fun `compensate restores stock and publishes compensated event`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Americano", price = 2000, quantity = 8),
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val useCase = CompensateOrderStockUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.compensate(requestedEvent()) {}

        assertEquals(10, itemRepository.findById(1L)!!.getQuantity())
        assertInstanceOf(OrderStockCompensatedEvent::class.java, eventPublisher.published.single())
    }

    @Test
    fun `compensate aggregates duplicated items before restoring stock`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Americano", price = 2000, quantity = 7),
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val useCase = CompensateOrderStockUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.compensate(
            requestedEvent(
                items = listOf(
                    OrderStockCompensationItemPayload(itemId = 1L, quantity = 1),
                    OrderStockCompensationItemPayload(itemId = 1L, quantity = 2),
                ),
            ),
        ) {}

        assertEquals(10, itemRepository.findById(1L)!!.getQuantity())
        assertInstanceOf(OrderStockCompensatedEvent::class.java, eventPublisher.published.single())
    }

    @Test
    fun `compensate publishes failed event when item is missing`() {
        val itemRepository = FakeItemRepository()
        val eventPublisher = FakeEventPublisher()
        val useCase = CompensateOrderStockUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.compensate(requestedEvent()) {}

        val publishedEvent = assertInstanceOf(OrderStockCompensationFailedEvent::class.java, eventPublisher.published.single())
        assertEquals("order-1", publishedEvent.orderId)
    }

    private fun requestedEvent(
        items: List<OrderStockCompensationItemPayload> = listOf(
            OrderStockCompensationItemPayload(itemId = 1L, quantity = 2),
        ),
    ): OrderStockCompensationRequestedEvent {
        return OrderStockCompensationRequestedEvent(
            orderId = "order-1",
            items = items,
        )
    }
}
