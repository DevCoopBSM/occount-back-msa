package devcoop.occount.item.application.query

import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.itemFixture
import devcoop.occount.item.domain.item.Category
import devcoop.occount.item.domain.item.ItemNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ItemQueryServiceTest {
    @Test
    fun `get all items returns active items`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Snack"),
                itemFixture(itemId = 2L, name = "Inactive", isActive = false),
            ),
        )
        val itemQueryService = ItemQueryService(itemRepository)

        val result = itemQueryService.getAllItems()

        assertEquals(1, result.items.size)
        assertEquals("Snack", result.items.single().name)
    }

    @Test
    fun `get item categories returns all categories`() {
        val itemQueryService = ItemQueryService(FakeItemRepository())

        val result = itemQueryService.getItemCategories()

        assertEquals(Category.entries, result.itemCategories)
    }

    @Test
    fun `get items without barcode returns only active items without barcode`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Snack"),
                itemFixture(itemId = 2L, name = "Drink", barcode = "88012341234"),
                itemFixture(itemId = 3L, name = "Inactive", isActive = false),
            ),
        )
        val itemQueryService = ItemQueryService(itemRepository)

        val result = itemQueryService.getItemsWithoutBarcode()

        assertEquals(1, result.items.size)
        assertEquals("Snack", result.items.single().name)
    }

    @Test
    fun `get item by barcode throws when item does not exist`() {
        val itemQueryService = ItemQueryService(FakeItemRepository())

        assertThrows(ItemNotFoundException::class.java) {
            itemQueryService.getItemByBarcode("88012341234")
        }
    }
}
