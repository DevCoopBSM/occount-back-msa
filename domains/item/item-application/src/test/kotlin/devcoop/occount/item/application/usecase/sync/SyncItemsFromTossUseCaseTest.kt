package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.output.TossItemPayload
import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.FakeTossItemPort
import devcoop.occount.item.application.support.itemFixture
import devcoop.occount.item.domain.item.Category
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SyncItemsFromTossUseCaseTest {
    @Test
    fun `sync stores only new items`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L, name = "Existing")),
        )
        val tossItemPort = FakeTossItemPort(
            itemPayloads = listOf(
                TossItemPayload(
                    itemId = 1L,
                    name = "Existing",
                    category = Category.과자,
                    price = 1500,
                ),
                TossItemPayload(
                    itemId = 2L,
                    name = "New",
                    category = Category.음료,
                    price = 2000,
                    barcode = "88012341234",
                ),
            ),
        )
        val syncItemsFromTossUseCase = SyncItemsFromTossUseCase(itemRepository, tossItemPort)

        syncItemsFromTossUseCase.sync()

        assertEquals(1, itemRepository.saveAllCount)
        assertEquals(1, itemRepository.lastSavedItems.size)
        assertEquals("New", itemRepository.findById(2L)!!.getName())
    }
}
