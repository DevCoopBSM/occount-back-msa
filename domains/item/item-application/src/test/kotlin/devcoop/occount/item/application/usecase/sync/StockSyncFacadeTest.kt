package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.output.SoldItemPayload
import devcoop.occount.item.application.output.TossItemPayload
import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.FakeTossItemPort
import devcoop.occount.item.application.support.TestTransactionManager
import devcoop.occount.item.application.support.itemFixture
import devcoop.occount.item.domain.item.Category
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StockSyncFacadeTest {
    @Test
    fun `apply returns immediately when there are no sold items`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L, name = "Snack", quantity = 10)),
        )
        val tossItemPort = FakeTossItemPort()
        val facade = stockSyncFacade(itemRepository, tossItemPort)

        facade.apply()

        assertEquals(0, tossItemPort.getItemsCount)
        assertEquals(0, itemRepository.saveAllCount)
    }

    @Test
    fun `apply syncs catalog before deducting when sold item name is missing locally`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(
                    itemId = 2L,
                    name = "Legacy Drink",
                    category = Category.식품,
                    price = 1000,
                    quantity = 5,
                ),
            ),
        )
        val tossItemPort = FakeTossItemPort(
            itemPayloads = listOf(
                TossItemPayload(
                    itemId = 2L,
                    name = "Drink",
                    category = Category.음료,
                    price = 2000,
                    barcode = "88055554444",
                ),
            ),
            soldItemPayloads = listOf(
                SoldItemPayload(name = "Drink", quantity = 2),
            ),
        )
        val facade = stockSyncFacade(itemRepository, tossItemPort)

        facade.apply()

        val updatedItem = itemRepository.findById(2L)!!
        assertEquals(1, tossItemPort.getItemsCount)
        assertEquals(2, itemRepository.saveAllCount)
        assertEquals("Drink", updatedItem.getName())
        assertEquals(3, updatedItem.getQuantity())
    }

    @Test
    fun `apply deducts sold quantities without catalog sync when sold item already exists locally`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L, name = "Snack", quantity = 10)),
        )
        val tossItemPort = FakeTossItemPort(
            soldItemPayloads = listOf(
                SoldItemPayload(name = "Snack", quantity = 2),
            ),
        )
        val facade = stockSyncFacade(itemRepository, tossItemPort)

        facade.apply()

        assertEquals(0, tossItemPort.getItemsCount)
        assertEquals(1, itemRepository.saveAllCount)
        assertEquals(8, itemRepository.findById(1L)!!.getQuantity())
    }

    private fun stockSyncFacade(
        itemRepository: FakeItemRepository,
        tossItemPort: FakeTossItemPort,
    ): StockSyncFacade {
        return StockSyncFacade(
            itemRepository = itemRepository,
            tossItemPort = tossItemPort,
            syncItemsFromTossUseCase = SyncItemsFromTossUseCase(itemRepository, tossItemPort),
            applySoldItemQuantitiesUseCase = ApplySoldItemQuantitiesUseCase(
                itemRepository = itemRepository,
                transactionManager = TestTransactionManager(),
            ),
        )
    }
}
