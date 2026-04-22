package devcoop.occount.item.application.usecase.aripick

import devcoop.occount.item.application.shared.AripickMapper
import devcoop.occount.item.application.support.FakeAripickRepository
import devcoop.occount.item.application.support.aripickFixture
import devcoop.occount.item.domain.aripick.AripickAccessDeniedException
import devcoop.occount.item.domain.aripick.AripickNotFoundException
import devcoop.occount.item.domain.aripick.AripickStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AripickCommandUseCaseTest {
    @Test
    fun `create stores proposal with requester as proposer`() {
        val repository = FakeAripickRepository()
        val useCase = AripickCommandUseCase(repository, AripickMapper())

        val result = useCase.create(
            request = CreateAripickRequest(name = "제로콜라", reason = "원함"),
            proposerId = 7L,
        )

        assertEquals(1L, result.proposalId)
        assertEquals(7L, result.proposerId)
        assertEquals(AripickStatus.검토중, result.status)
    }

    @Test
    fun `approve changes status to approved`() {
        val repository = FakeAripickRepository(initialItems = listOf(aripickFixture(proposalId = 1L)))
        val useCase = AripickCommandUseCase(repository, AripickMapper())

        val result = useCase.approve(1L)

        assertEquals(AripickStatus.승인됨, result.status)
    }

    @Test
    fun `reject changes status to rejected`() {
        val repository = FakeAripickRepository(initialItems = listOf(aripickFixture(proposalId = 1L)))
        val useCase = AripickCommandUseCase(repository, AripickMapper())

        val result = useCase.reject(1L)

        assertEquals(AripickStatus.거절됨, result.status)
    }

    @Test
    fun `delete throws when requester is not proposer`() {
        val repository = FakeAripickRepository(
            initialItems = listOf(aripickFixture(proposalId = 1L, proposerId = 2L)),
        )
        val useCase = AripickCommandUseCase(repository, AripickMapper())

        assertThrows(AripickAccessDeniedException::class.java) {
            useCase.delete(proposalId = 1L, requesterId = 9L)
        }
    }

    @Test
    fun `delete removes likes and proposal when requester is proposer`() {
        val repository = FakeAripickRepository(
            initialItems = listOf(aripickFixture(proposalId = 1L, proposerId = 7L)),
        ).apply {
            saveLikeIfAbsent(1L, 7L)
            saveLikeIfAbsent(1L, 8L)
        }
        val useCase = AripickCommandUseCase(repository, AripickMapper())

        useCase.delete(proposalId = 1L, requesterId = 7L)

        assertTrue(repository.deletedLikeProposalIds.contains(1L))
        assertTrue(repository.deletedProposalIds.contains(1L))
        assertEquals(null, repository.findById(1L))
    }

    @Test
    fun `delete as admin removes proposal regardless of proposer`() {
        val repository = FakeAripickRepository(
            initialItems = listOf(aripickFixture(proposalId = 1L, proposerId = 99L)),
        )
        val useCase = AripickCommandUseCase(repository, AripickMapper())

        useCase.deleteAsAdmin(1L)

        assertTrue(repository.deletedProposalIds.contains(1L))
    }

    @Test
    fun `toggle like stores like and increments count when not liked`() {
        val repository = FakeAripickRepository(
            initialItems = listOf(aripickFixture(proposalId = 1L, like = 0)),
        )
        val useCase = AripickCommandUseCase(repository, AripickMapper())

        val result = useCase.toggleLike(proposalId = 1L, userId = 7L)

        assertTrue(result.liked)
        assertEquals(1, result.likeCount)
        assertTrue(repository.existsLike(1L, 7L))
    }

    @Test
    fun `toggle like removes like and decrements count when already liked`() {
        val repository = FakeAripickRepository(
            initialItems = listOf(aripickFixture(proposalId = 1L, like = 1)),
        ).apply {
            saveLikeIfAbsent(1L, 7L)
        }
        val useCase = AripickCommandUseCase(repository, AripickMapper())

        val result = useCase.toggleLike(proposalId = 1L, userId = 7L)

        assertFalse(result.liked)
        assertEquals(0, result.likeCount)
        assertFalse(repository.existsLike(1L, 7L))
    }

    @Test
    fun `pending changes status to pending`() {
        val repository = FakeAripickRepository(
            initialItems = listOf(aripickFixture(proposalId = 1L, status = AripickStatus.승인됨)),
        )
        val useCase = AripickCommandUseCase(repository, AripickMapper())

        val result = useCase.pending(1L)

        assertEquals(AripickStatus.검토중, result.status)
    }

    @Test
    fun `command throws when proposal does not exist`() {
        val useCase = AripickCommandUseCase(FakeAripickRepository(), AripickMapper())

        assertThrows(AripickNotFoundException::class.java) {
            useCase.approve(100L)
        }
    }
}
