package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.output.SoldItemPayload
import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.FakeTossItemPort
import devcoop.occount.item.application.support.TestTransactionManager
import devcoop.occount.item.application.support.itemFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StockSyncSchedulerTest {
    @Test
    fun `sync delegates to stock sync facade`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L, name = "Snack", quantity = 10)),
        )
        val tossItemPort = FakeTossItemPort(
            soldItemPayloads = listOf(
                SoldItemPayload(name = "Snack", quantity = 2),
            ),
        )
        val scheduler = StockSyncScheduler(
            stockSyncFacade = StockSyncFacade(
                itemRepository = itemRepository,
                tossItemPort = tossItemPort,
                syncItemsFromTossUseCase = SyncItemsFromTossUseCase(itemRepository, tossItemPort),
                applySoldItemQuantitiesUseCase = ApplySoldItemQuantitiesUseCase(
                    itemRepository = itemRepository,
                    transactionManager = TestTransactionManager(),
                ),
            ),
        )

        scheduler.sync()

        assertEquals(8, itemRepository.findById(1L)!!.getQuantity())
    }
}
