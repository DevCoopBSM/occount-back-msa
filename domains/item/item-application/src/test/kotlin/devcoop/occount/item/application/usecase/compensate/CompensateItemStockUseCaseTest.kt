package devcoop.occount.item.application.usecase.compensate

import devcoop.occount.core.common.event.ItemStockCompensatedEvent
import devcoop.occount.core.common.event.ItemStockCompensationFailedEvent
import devcoop.occount.core.common.event.ItemStockCompensationPayload
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
import devcoop.occount.item.application.support.FakeEventPublisher
import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.TestTransactionManager
import devcoop.occount.item.application.support.itemFixture
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CompensateItemStockUseCaseTest {
    @Test
    fun `compensate restores stock and publishes compensated event`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Americano", price = 2000, quantity = 8),
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val useCase = CompensateItemStockUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.compensate(requestedEvent()) {}

        Assertions.assertEquals(10, itemRepository.findById(1L)!!.getQuantity())
        Assertions.assertInstanceOf(ItemStockCompensatedEvent::class.java, eventPublisher.published.single())
    }

    @Test
    fun `compensate aggregates duplicated items before restoring stock`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Americano", price = 2000, quantity = 7),
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val useCase = CompensateItemStockUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.compensate(
            requestedEvent(
                items = listOf(
                    ItemStockCompensationPayload(itemId = 1L, quantity = 1),
                    ItemStockCompensationPayload(itemId = 1L, quantity = 2),
                ),
            ),
        ) {}

        Assertions.assertEquals(10, itemRepository.findById(1L)!!.getQuantity())
        Assertions.assertInstanceOf(ItemStockCompensatedEvent::class.java, eventPublisher.published.single())
    }

    @Test
    fun `compensate publishes failed event when item is missing`() {
        val itemRepository = FakeItemRepository()
        val eventPublisher = FakeEventPublisher()
        val useCase = CompensateItemStockUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.compensate(requestedEvent()) {}

        val publishedEvent = Assertions.assertInstanceOf(
            ItemStockCompensationFailedEvent::class.java,
            eventPublisher.published.single()
        )
        Assertions.assertEquals("order-1", publishedEvent.orderId)
    }

    private fun requestedEvent(
        items: List<ItemStockCompensationPayload> = listOf(
            ItemStockCompensationPayload(itemId = 1L, quantity = 2),
        ),
    ): OrderStockCompensationRequestedEvent {
        return OrderStockCompensationRequestedEvent(
            orderId = "order-1",
            items = items,
        )
    }
}
