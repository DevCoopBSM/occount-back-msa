package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.output.SoldItemPayload
import devcoop.occount.item.application.output.TossItemPayload
import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.FakeTossItemPort
import devcoop.occount.item.application.support.itemFixture
import devcoop.occount.item.domain.item.Category
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApplySoldItemQuantitiesUseCaseTest {
    @Test
    fun `apply returns immediately when there are no sold items`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L, quantity = 10)),
        )
        val tossItemPort = FakeTossItemPort()
        val syncItemsFromTossUseCase = SyncItemsFromTossUseCase(itemRepository, tossItemPort)
        val applySoldItemQuantitiesUseCase = ApplySoldItemQuantitiesUseCase(
            itemRepository = itemRepository,
            tossItemPort = tossItemPort,
            syncItemsFromTossUseCase = syncItemsFromTossUseCase,
        )

        applySoldItemQuantitiesUseCase.apply()

        assertEquals(0, itemRepository.saveAllCount)
    }

    @Test
    fun `apply syncs catalog when repository contains names outside sold items and then deducts quantities`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Snack", quantity = 10),
                itemFixture(itemId = 3L, name = "Legacy", quantity = 5),
            ),
        )
        val tossItemPort = FakeTossItemPort(
            itemPayloads = listOf(
                TossItemPayload(
                    itemId = 1L,
                    name = "Snack",
                    category = Category.과자,
                    price = 1500,
                ),
                TossItemPayload(
                    itemId = 2L,
                    name = "Drink",
                    category = Category.음료,
                    price = 2000,
                ),
                TossItemPayload(
                    itemId = 3L,
                    name = "Legacy",
                    category = Category.식품,
                    price = 1000,
                ),
            ),
            soldItemPayloads = listOf(
                SoldItemPayload(
                    name = "Snack",
                    quantity = 2,
                ),
            ),
        )
        val syncItemsFromTossUseCase = SyncItemsFromTossUseCase(itemRepository, tossItemPort)
        val applySoldItemQuantitiesUseCase = ApplySoldItemQuantitiesUseCase(
            itemRepository = itemRepository,
            tossItemPort = tossItemPort,
            syncItemsFromTossUseCase = syncItemsFromTossUseCase,
        )

        applySoldItemQuantitiesUseCase.apply()

        assertEquals(2, itemRepository.saveAllCount)
        assertEquals(8, itemRepository.findById(1L)!!.getQuantity())
        assertEquals("Drink", itemRepository.findById(2L)!!.getName())
    }
}
