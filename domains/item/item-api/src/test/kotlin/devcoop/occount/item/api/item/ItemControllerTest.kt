package devcoop.occount.item.api.item

import devcoop.occount.item.application.query.ItemCategoryListResponse
import devcoop.occount.item.application.query.ItemListResponse
import devcoop.occount.item.application.query.ItemLookupListResponse
import devcoop.occount.item.application.query.ItemQueryService
import devcoop.occount.item.application.shared.ItemLookupResponse
import devcoop.occount.item.application.shared.ItemResponse
import devcoop.occount.item.application.usecase.delete.DeleteItemUseCase
import devcoop.occount.item.application.usecase.sync.SyncItemsFromTossUseCase
import devcoop.occount.item.application.usecase.update.ItemUpdateRequest
import devcoop.occount.item.application.usecase.update.UpdateItemUseCase
import devcoop.occount.item.domain.item.Category
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ItemControllerTest {
    @Test
    fun `get all items delegates to item query service`() {
        val itemQueryService = mock(ItemQueryService::class.java)
        val controller = controller(itemQueryService = itemQueryService)
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

        `when`(itemQueryService.getAllItems()).thenReturn(expected)

        val actual = controller.getAllItems()

        assertSame(expected, actual)
        verify(itemQueryService).getAllItems()
    }

    @Test
    fun `get item categories delegates to item query service`() {
        val itemQueryService = mock(ItemQueryService::class.java)
        val controller = controller(itemQueryService = itemQueryService)
        val expected = ItemCategoryListResponse(Category.entries)

        `when`(itemQueryService.getItemCategories()).thenReturn(expected)

        val actual = controller.getItemCategories()

        assertSame(expected, actual)
        verify(itemQueryService).getItemCategories()
    }

    @Test
    fun `get items without barcode delegates to item query service`() {
        val itemQueryService = mock(ItemQueryService::class.java)
        val controller = controller(itemQueryService = itemQueryService)
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

        `when`(itemQueryService.getItemsWithoutBarcode()).thenReturn(expected)

        val actual = controller.getItemsWithoutBarcode()

        assertSame(expected, actual)
        verify(itemQueryService).getItemsWithoutBarcode()
    }

    @Test
    fun `get item by barcode delegates to item query service`() {
        val itemQueryService = mock(ItemQueryService::class.java)
        val controller = controller(itemQueryService = itemQueryService)
        val expected = ItemLookupResponse(
            itemId = 1L,
            name = "Snack",
            barcode = "88012341234",
            price = 1500,
        )

        `when`(itemQueryService.getItemByBarcode("88012341234")).thenReturn(expected)

        val actual = controller.getItemByBarcode("88012341234")

        assertSame(expected, actual)
        verify(itemQueryService).getItemByBarcode("88012341234")
    }

    @Test
    fun `sync delegates to sync items from toss use case`() {
        val syncItemsFromTossUseCase = mock(SyncItemsFromTossUseCase::class.java)
        val controller = controller(syncItemsFromTossUseCase = syncItemsFromTossUseCase)

        controller.syncItemsFromToss()

        verify(syncItemsFromTossUseCase).sync()
    }

    @Test
    fun `update delegates to update item use case`() {
        val updateItemUseCase = mock(UpdateItemUseCase::class.java)
        val controller = controller(updateItemUseCase = updateItemUseCase)
        val request = ItemUpdateRequest(
            name = "Snack",
            category = Category.과자,
            price = 1500,
            barcode = "88012341234",
            quantity = 10,
        )
        val expected = ItemResponse(
            itemId = 1L,
            name = "Snack",
            category = Category.과자,
            price = 1500,
            barcode = "88012341234",
        )

        `when`(updateItemUseCase.update(1L, request)).thenReturn(expected)

        val actual = controller.updateItem(1L, request)

        assertSame(expected, actual)
        verify(updateItemUseCase).update(1L, request)
    }

    @Test
    fun `delete delegates to delete item use case`() {
        val deleteItemUseCase = mock(DeleteItemUseCase::class.java)
        val controller = controller(deleteItemUseCase = deleteItemUseCase)

        controller.deleteItem(1L)

        verify(deleteItemUseCase).delete(1L)
    }

    private fun controller(
        itemQueryService: ItemQueryService = mock(ItemQueryService::class.java),
        syncItemsFromTossUseCase: SyncItemsFromTossUseCase = mock(SyncItemsFromTossUseCase::class.java),
        updateItemUseCase: UpdateItemUseCase = mock(UpdateItemUseCase::class.java),
        deleteItemUseCase: DeleteItemUseCase = mock(DeleteItemUseCase::class.java),
    ): ItemController {
        return ItemController(
            itemQueryService = itemQueryService,
            syncItemsFromTossUseCase = syncItemsFromTossUseCase,
            updateItemUseCase = updateItemUseCase,
            deleteItemUseCase = deleteItemUseCase,
        )
    }
}
