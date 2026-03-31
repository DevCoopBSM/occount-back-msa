package devcoop.occount.item.application.usecase.delete

import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.itemFixture
import devcoop.occount.item.domain.item.ItemNotFoundException
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class DeleteItemUseCaseTest {
    @Test
    fun `delete deactivates item`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L)),
        )
        val deleteItemUseCase = DeleteItemUseCase(itemRepository)

        deleteItemUseCase.delete(1L)

        assertFalse(itemRepository.findById(1L)!!.isActive())
    }

    @Test
    fun `delete throws when item does not exist`() {
        val deleteItemUseCase = DeleteItemUseCase(FakeItemRepository())

        assertThrows(ItemNotFoundException::class.java) {
            deleteItemUseCase.delete(1L)
        }
    }
}
