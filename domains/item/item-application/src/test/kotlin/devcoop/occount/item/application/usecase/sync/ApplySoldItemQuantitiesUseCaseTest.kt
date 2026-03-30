package devcoop.occount.item.application.usecase.sync

import devcoop.occount.item.application.exception.ItemNotSynchronizedException
import devcoop.occount.item.application.output.SoldItemPayload
import devcoop.occount.item.application.support.FakeItemRepository
import devcoop.occount.item.application.support.TestTransactionManager
import devcoop.occount.item.application.support.itemFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.dao.OptimisticLockingFailureException

class ApplySoldItemQuantitiesUseCaseTest {
    @Test
    fun `apply returns immediately when there are no sold items`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L, quantity = 10)),
        )
        val useCase = ApplySoldItemQuantitiesUseCase(
            itemRepository = itemRepository,
            transactionManager = TestTransactionManager(),
        )

        useCase.apply(emptyList())

        assertEquals(0, itemRepository.saveAllCount)
    }

    @Test
    fun `apply aggregates sold quantities by item name`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L, name = "Snack", quantity = 10)),
        )
        val useCase = ApplySoldItemQuantitiesUseCase(
            itemRepository = itemRepository,
            transactionManager = TestTransactionManager(),
        )

        useCase.apply(
            listOf(
                SoldItemPayload(name = "Snack", quantity = 2),
                SoldItemPayload(name = "Snack", quantity = 3),
            ),
        )

        assertEquals(1, itemRepository.saveAllCount)
        assertEquals(5, itemRepository.findById(1L)!!.getQuantity())
    }

    @Test
    fun `apply retries when optimistic lock failure occurs`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L, name = "Snack", quantity = 10)),
        ).apply {
            saveAllOptimisticLockFailuresRemaining = 1
        }
        val useCase = ApplySoldItemQuantitiesUseCase(
            itemRepository = itemRepository,
            transactionManager = TestTransactionManager(),
        )

        useCase.apply(
            listOf(SoldItemPayload(name = "Snack", quantity = 2)),
        )

        assertEquals(2, itemRepository.saveAllCount)
        assertEquals(8, itemRepository.findById(1L)!!.getQuantity())
    }

    @Test
    fun `apply throws when sold item is not synchronized`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L, name = "Snack", quantity = 10)),
        )
        val useCase = ApplySoldItemQuantitiesUseCase(
            itemRepository = itemRepository,
            transactionManager = TestTransactionManager(),
        )

        assertThrows(ItemNotSynchronizedException::class.java) {
            useCase.apply(
                listOf(SoldItemPayload(name = "Drink", quantity = 2)),
            )
        }
    }

    @Test
    fun `apply throws after three optimistic lock failures`() {
        val itemRepository = FakeItemRepository(
            initialItems = listOf(itemFixture(itemId = 1L, name = "Snack", quantity = 10)),
        ).apply {
            saveAllOptimisticLockFailuresRemaining = 3
        }
        val useCase = ApplySoldItemQuantitiesUseCase(
            itemRepository = itemRepository,
            transactionManager = TestTransactionManager(),
        )

        assertThrows(OptimisticLockingFailureException::class.java) {
            useCase.apply(
                listOf(SoldItemPayload(name = "Snack", quantity = 2)),
            )
        }
        assertEquals(3, itemRepository.saveAllCount)
        assertEquals(10, itemRepository.findById(1L)!!.getQuantity())
    }
}
