package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.output.TossItemPayload
import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.FakeTossItemPort
import devcoop.occount.item.application.support.TestTransactionManager
import devcoop.occount.item.application.support.itemFixture
import devcoop.occount.item.domain.item.Category
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SyncItemsFromTossUseCaseTest {
    @Test
    fun `sync upserts catalog and preserves stock state`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(
                    itemId = 1L,
                    name = "Legacy Snack",
                    category = Category.식품,
                    price = 1000,
                    quantity = 7,
                    isActive = false,
                    catalogVersion = 3L,
                ),
            ),
        )
        val tossItemPort = FakeTossItemPort(
            itemPayloads = listOf(
                TossItemPayload(
                    itemId = 1L,
                    name = "Snack",
                    category = Category.과자,
                    price = 1500,
                    barcode = "88012341234",
                ),
                TossItemPayload(
                    itemId = 2L,
                    name = "Drink",
                    category = Category.음료,
                    price = 2000,
                    barcode = "88055554444",
                ),
            ),
        )
        val useCase = SyncItemsFromTossUseCase(itemRepository, tossItemPort, TestTransactionManager())

        useCase.sync()

        val updatedExistingItem = itemRepository.findById(1L)!!
        val newItem = itemRepository.findById(2L)!!

        assertEquals(1, itemRepository.saveCatalogCount)
        assertEquals(1, itemRepository.saveCatalogsCount)
        assertEquals(1, itemRepository.lastSavedCatalogItems.size)
        assertEquals("Snack", updatedExistingItem.getName())
        assertEquals(Category.과자, updatedExistingItem.getCategory())
        assertEquals(1500, updatedExistingItem.getPrice())
        assertEquals("88012341234", updatedExistingItem.getBarcode())
        assertEquals(7, updatedExistingItem.getQuantity())
        assertFalse(updatedExistingItem.isActive())
        assertEquals("Drink", newItem.getName())
        assertEquals(0, newItem.getQuantity())
    }

    @Test
    fun `sync returns immediately when toss items are empty`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L, name = "Snack", quantity = 10)),
        )
        val tossItemPort = FakeTossItemPort()
        val useCase = SyncItemsFromTossUseCase(itemRepository, tossItemPort, TestTransactionManager())

        useCase.sync()

        assertEquals(0, itemRepository.saveCatalogCount)
        assertEquals(0, itemRepository.saveCatalogsCount)
    }

    @Test
    fun `sync skips unchanged catalog`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(
                    itemId = 1L,
                    name = "Snack",
                    category = Category.과자,
                    price = 1500,
                    barcode = "88012341234",
                    quantity = 7,
                ),
            ),
        )
        val tossItemPort = FakeTossItemPort(
            itemPayloads = listOf(
                TossItemPayload(
                    itemId = 1L,
                    name = "Snack",
                    category = Category.과자,
                    price = 1500,
                    barcode = "88012341234",
                ),
            ),
        )
        val useCase = SyncItemsFromTossUseCase(itemRepository, tossItemPort, TestTransactionManager())

        useCase.sync()

        assertEquals(0, itemRepository.saveCatalogCount)
        assertEquals(0, itemRepository.saveCatalogsCount)
    }

    @Test
    fun `sync retries catalog save when optimistic lock failure occurs`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Legacy Snack", quantity = 7),
            ),
        ).apply {
            saveCatalogOptimisticLockFailuresRemaining = 1
        }
        val tossItemPort = FakeTossItemPort(
            itemPayloads = listOf(
                TossItemPayload(
                    itemId = 1L,
                    name = "Snack",
                    category = Category.과자,
                    price = 1500,
                    barcode = null,
                ),
            ),
        )
        val useCase = SyncItemsFromTossUseCase(itemRepository, tossItemPort, TestTransactionManager())

        useCase.sync()

        assertEquals(2, itemRepository.saveCatalogCount)
        assertEquals("Snack", itemRepository.findById(1L)!!.getName())
    }
}
