package devcoop.occount.suggestion.application.query

import devcoop.occount.suggestion.application.shared.AripickMapper
import devcoop.occount.suggestion.application.support.FakeAripickRepository
import devcoop.occount.suggestion.application.support.aripickFixture
import devcoop.occount.suggestion.domain.aripick.AripickNotFoundException
import devcoop.occount.suggestion.domain.aripick.AripickStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AripickQueryServiceTest {
    @Test
    fun `get all items returns mapped list`() {
        val repository = FakeAripickRepository(
            initialItems = listOf(
                aripickFixture(proposalId = 1L, name = "제로콜라"),
                aripickFixture(proposalId = 2L, name = "삼각김밥", like = 3),
            ),
        )
        val service = AripickQueryService(repository, AripickMapper())

        val result = service.getAllItems()

        assertEquals(2, result.aripickItems.size)
        assertEquals("제로콜라", result.aripickItems.first().name)
        assertEquals(3, result.aripickItems.last().likeCount)
    }

    @Test
    fun `get item throws when proposal does not exist`() {
        val service = AripickQueryService(FakeAripickRepository(), AripickMapper())

        assertThrows(AripickNotFoundException::class.java) {
            service.getItem(10L)
        }
    }

    @Test
    fun `get stats aggregates by status`() {
        val repository = FakeAripickRepository(
            initialItems = listOf(
                aripickFixture(proposalId = 1L, status = AripickStatus.검토중),
                aripickFixture(proposalId = 2L, status = AripickStatus.승인됨),
                aripickFixture(proposalId = 3L, status = AripickStatus.거절됨),
                aripickFixture(proposalId = 4L, status = AripickStatus.승인됨),
            ),
        )
        val service = AripickQueryService(repository, AripickMapper())

        val result = service.getStats()

        assertEquals(4L, result.totalProposals)
        assertEquals(2L, result.approved)
        assertEquals(1L, result.pending)
        assertEquals(1L, result.rejected)
    }
}
