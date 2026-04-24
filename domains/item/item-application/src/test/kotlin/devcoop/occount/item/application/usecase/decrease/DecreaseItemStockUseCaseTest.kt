package devcoop.occount.item.application.usecase.decrease

import devcoop.occount.core.common.event.ItemStockPayload
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.core.common.event.OrderRequestedItemPayload
import devcoop.occount.core.common.event.ItemStockDecreasedEvent
import devcoop.occount.core.common.event.ItemStockDecreaseFailedEvent
import devcoop.occount.item.application.support.FakeEventPublisher
import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.TestTransactionManager
import devcoop.occount.item.application.support.itemFixture
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class DecreaseItemStockUseCaseTest {
    @Test
    fun `decrease reduces stock and publishes stock completed`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Americano", price = 2000, quantity = 10),
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val useCase = DecreaseItemStockUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.decrease(requestedEvent()) {}

        Assertions.assertEquals(8, itemRepository.findById(1L)!!.getQuantity())
        val publishedEvent =
            Assertions.assertInstanceOf(ItemStockDecreasedEvent::class.java, eventPublisher.published.single())
        Assertions.assertEquals(4000, publishedEvent.totalAmount)
        Assertions.assertEquals(
            listOf(
                ItemStockPayload(
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
    fun `decrease aggregates duplicated item requests before deducting stock`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Americano", price = 2000, quantity = 10),
            ),
        )
        val eventPublisher = FakeEventPublisher()
        val useCase = DecreaseItemStockUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.decrease(
            requestedEvent(
                items = listOf(
                    OrderRequestedItemPayload(itemId = 1L, quantity = 1),
                    OrderRequestedItemPayload(itemId = 1L, quantity = 2),
                ),
            ),
        ) {}

        Assertions.assertEquals(7, itemRepository.findById(1L)!!.getQuantity())
        val publishedEvent =
            Assertions.assertInstanceOf(ItemStockDecreasedEvent::class.java, eventPublisher.published.single())
        Assertions.assertEquals(6000, publishedEvent.totalAmount)
        Assertions.assertEquals(1, publishedEvent.items.size)
        Assertions.assertEquals(3, publishedEvent.items.single().quantity)
    }

    @Test
    fun `decrease publishes stock failed when item is missing`() {
        val itemRepository = FakeItemRepository()
        val eventPublisher = FakeEventPublisher()
        val useCase = DecreaseItemStockUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.decrease(requestedEvent()) {}

        val publishedEvent =
            Assertions.assertInstanceOf(ItemStockDecreaseFailedEvent::class.java, eventPublisher.published.single())
        Assertions.assertEquals("order-1", publishedEvent.orderId)
    }

    @Test
    fun `decrease retries optimistic lock failures before succeeding`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Americano", price = 2000, quantity = 10),
            ),
        ).apply {
            saveStockOptimisticLockFailuresRemaining = 1
        }
        val eventPublisher = FakeEventPublisher()
        val useCase = DecreaseItemStockUseCase(
            itemRepository = itemRepository,
            eventPublisher = eventPublisher,
            transactionManager = TestTransactionManager(),
        )

        useCase.decrease(requestedEvent()) {}

        Assertions.assertEquals(2, itemRepository.saveStocksCount)
        Assertions.assertInstanceOf(ItemStockDecreasedEvent::class.java, eventPublisher.published.single())
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
