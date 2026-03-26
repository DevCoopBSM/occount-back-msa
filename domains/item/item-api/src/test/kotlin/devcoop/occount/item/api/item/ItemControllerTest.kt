package devcoop.occount.item.api.item

import devcoop.occount.item.application.item.ItemListResponse
import devcoop.occount.item.application.item.ItemLookupListResponse
import devcoop.occount.item.application.item.ItemLookupResponse
import devcoop.occount.item.application.item.ItemResponse
import devcoop.occount.item.application.item.ItemService
import devcoop.occount.item.domain.item.Category
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ItemControllerTest {
    @Test
    fun `get all items delegates to item service`() {
        val itemService = mock(ItemService::class.java)
        val controller = ItemController(itemService)
        val expected = ItemListResponse(
            items = listOf(
                ItemResponse(
                    itemId = 1L,
                    name = "Snack",
                    category = Category.과자,
                    price = 1500,
                ),
            ),
        )

        `when`(itemService.getAllItems()).thenReturn(expected)

        val actual = controller.getAllItems()

        assertSame(expected, actual)
        verify(itemService).getAllItems()
    }

    @Test
    fun `get items without barcode delegates to item service`() {
        val itemService = mock(ItemService::class.java)
        val controller = ItemController(itemService)
        val expected = ItemLookupListResponse(
            items = listOf(
                ItemLookupResponse(
                    itemId = 1L,
                    name = "Snack",
                    barcode = null,
                    price = 1500,
                ),
            ),
        )

        `when`(itemService.getItemsWithoutBarcode()).thenReturn(expected)

        val actual = controller.getItemsWithoutBarcode()

        assertSame(expected, actual)
        verify(itemService).getItemsWithoutBarcode()
    }

    @Test
    fun `get item by barcode delegates to item service`() {
        val itemService = mock(ItemService::class.java)
        val controller = ItemController(itemService)
        val expected = ItemLookupResponse(
            itemId = 1L,
            name = "Snack",
            barcode = "88012341234",
            price = 1500,
        )

        `when`(itemService.getItemByBarcode("88012341234")).thenReturn(expected)

        val actual = controller.getItemByBarcode("88012341234")

        assertSame(expected, actual)
        verify(itemService).getItemByBarcode("88012341234")
    }

    @Test
    fun `sync delegates to item service`() {
        val itemService = mock(ItemService::class.java)
        val controller = ItemController(itemService)

        controller.syncItemsFromToss()

        verify(itemService).syncItemsFromToss()
    }
}
