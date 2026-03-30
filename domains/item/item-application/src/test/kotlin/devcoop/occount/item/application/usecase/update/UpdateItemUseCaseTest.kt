package devcoop.occount.item.application.usecase.update

import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.TestTransactionManager
import devcoop.occount.item.application.support.itemFixture
import devcoop.occount.item.domain.item.Category
import devcoop.occount.item.domain.item.ItemNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UpdateItemUseCaseTest {
    @Test
    fun `update changes item fields and preserves inactive state`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Old", quantity = 3, isActive = false),
            ),
        )
        val updateItemUseCase = UpdateItemUseCase(itemRepository, TestTransactionManager())

        val result = updateItemUseCase.update(
            id = 1L,
            request = ItemUpdateRequest(
                name = "New",
                category = Category.음료,
                price = 2000,
                barcode = "88012341234",
                quantity = 10,
            ),
        )

        val updatedItem = itemRepository.findById(1L)!!
        assertEquals("New", result.name)
        assertEquals(10, updatedItem.getQuantity())
        assertFalse(updatedItem.isActive())
    }

    @Test
    fun `update throws when item does not exist`() {
        val updateItemUseCase = UpdateItemUseCase(FakeItemRepository(), TestTransactionManager())

        assertThrows(ItemNotFoundException::class.java) {
            updateItemUseCase.update(
                id = 1L,
                request = ItemUpdateRequest(
                    name = "Snack",
                    category = Category.과자,
                    price = 1500,
                    barcode = null,
                    quantity = 1,
                ),
            )
        }
    }

    @Test
    fun `update retries stock save when optimistic lock failure occurs`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(
                itemFixture(itemId = 1L, name = "Snack", quantity = 3),
            ),
        ).apply {
            saveStockOptimisticLockFailuresRemaining = 1
        }
        val updateItemUseCase = UpdateItemUseCase(itemRepository, TestTransactionManager())

        val result = updateItemUseCase.update(
            id = 1L,
            request = ItemUpdateRequest(
                name = "Snack",
                category = Category.과자,
                price = 1500,
                barcode = null,
                quantity = 5,
            ),
        )

        assertEquals(2, itemRepository.saveStockCount)
        assertEquals(5, itemRepository.findById(1L)!!.getQuantity())
        assertEquals("Snack", result.name)
    }
}
