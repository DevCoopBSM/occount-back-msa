package devcoop.occount.suggestion.application.usecase.aripick

import devcoop.occount.suggestion.application.shared.AripickMapper
import devcoop.occount.suggestion.application.support.FakeAripickPolicyRepository
import devcoop.occount.suggestion.application.support.FakeAripickRepository
import devcoop.occount.suggestion.application.support.FakeFoodSafetyRepository
import devcoop.occount.suggestion.application.support.aripickFixture
import devcoop.occount.suggestion.domain.aripick.AripickBlockedKeyword
import devcoop.occount.suggestion.domain.aripick.AripickAccessDeniedException
import devcoop.occount.suggestion.domain.aripick.AripickFoodSafetyUnavailableException
import devcoop.occount.suggestion.domain.aripick.AripickNotFoundException
import devcoop.occount.suggestion.domain.aripick.AripickPolicyViolationException
import devcoop.occount.suggestion.domain.aripick.AripickStatus
import devcoop.occount.suggestion.application.output.FoodSafetyProductDetail
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AripickCommandUseCaseTest {
    @Test
    fun `create stores proposal with requester as proposer`() {
        val repository = FakeAripickRepository()
        val useCase = AripickCommandUseCase(
            repository,
            FakeAripickPolicyRepository(),
            FakeFoodSafetyRepository(
                detailsByTypeNSeq = mapOf(
                    14116L to FoodSafetyProductDetail(
                        typeNSeq = 14116L,
                        name = "신라면 큰사발면",
                        isAllowed = true,
                    ),
                ),
            ),
            AripickMapper(),
        )

        val result = useCase.create(
            request = CreateAripickRequest(typeNSeq = 14116L, reason = "원함"),
            proposerId = 7L,
        )

        assertEquals(1L, result.proposalId)
        assertEquals(7L, result.proposerId)
        assertEquals(AripickStatus.검토중, result.status)
        assertEquals("신라면 큰사발면", result.name)
    }

    @Test
    fun `create throws when food safety result is not allowed`() {
        val useCase = AripickCommandUseCase(
            FakeAripickRepository(),
            FakeAripickPolicyRepository(),
            FakeFoodSafetyRepository(
                detailsByTypeNSeq = mapOf(
                    14116L to FoodSafetyProductDetail(
                        typeNSeq = 14116L,
                        name = "신라면 큰사발면",
                        isAllowed = false,
                    ),
                ),
            ),
            AripickMapper(),
        )

        assertThrows(AripickPolicyViolationException::class.java) {
            useCase.create(
                request = CreateAripickRequest(typeNSeq = 14116L, reason = "원함"),
                proposerId = 7L,
            )
        }
    }

    @Test
    fun `create throws when product name contains blocked keyword`() {
        val useCase = AripickCommandUseCase(
            FakeAripickRepository(),
            FakeAripickPolicyRepository(
                initialKeywords = listOf(
                    AripickBlockedKeyword(keywordId = 1L, keyword = "에너지"),
                ),
            ),
            FakeFoodSafetyRepository(
                detailsByTypeNSeq = mapOf(
                    14116L to FoodSafetyProductDetail(
                        typeNSeq = 14116L,
                        name = "에너지 드링크 제로",
                        isAllowed = true,
                    ),
                ),
            ),
            AripickMapper(),
        )

        assertThrows(AripickPolicyViolationException::class.java) {
            useCase.create(
                request = CreateAripickRequest(typeNSeq = 14116L, reason = "원함"),
                proposerId = 7L,
            )
        }
    }

    @Test
    fun `create throws when food safety service is unavailable`() {
        val useCase = AripickCommandUseCase(
            FakeAripickRepository(),
            FakeAripickPolicyRepository(),
            FakeFoodSafetyRepository(throwOnDetail = AripickFoodSafetyUnavailableException()),
            AripickMapper(),
        )

        assertThrows(AripickFoodSafetyUnavailableException::class.java) {
            useCase.create(
                request = CreateAripickRequest(typeNSeq = 14116L, reason = "원함"),
                proposerId = 7L,
            )
        }
    }

    @Test
    fun `create throws when food safety detail is missing`() {
        val useCase = AripickCommandUseCase(
            FakeAripickRepository(),
            FakeAripickPolicyRepository(),
            FakeFoodSafetyRepository(detailsByTypeNSeq = emptyMap()),
            AripickMapper(),
        )

        assertThrows(AripickFoodSafetyUnavailableException::class.java) {
            useCase.create(
                request = CreateAripickRequest(typeNSeq = 14116L, reason = "원함"),
                proposerId = 7L,
            )
        }
    }

    @Test
    fun `approve changes status to approved`() {
        val repository = FakeAripickRepository(initialItems = listOf(aripickFixture(proposalId = 1L)))
        val useCase = AripickCommandUseCase(repository, FakeAripickPolicyRepository(), FakeFoodSafetyRepository(), AripickMapper())

        val result = useCase.approve(1L)

        assertEquals(AripickStatus.승인됨, result.status)
    }

    @Test
    fun `approve throws when status update fails`() {
        val repository = FakeAripickRepository(
            initialItems = listOf(aripickFixture(proposalId = 1L)),
            statusUpdateFailIds = setOf(1L),
        )
        val useCase = AripickCommandUseCase(repository, FakeAripickPolicyRepository(), FakeFoodSafetyRepository(), AripickMapper())

        assertThrows(AripickNotFoundException::class.java) {
            useCase.approve(1L)
        }
    }

    @Test
    fun `reject changes status to rejected`() {
        val repository = FakeAripickRepository(initialItems = listOf(aripickFixture(proposalId = 1L)))
        val useCase = AripickCommandUseCase(repository, FakeAripickPolicyRepository(), FakeFoodSafetyRepository(), AripickMapper())

        val result = useCase.reject(1L)

        assertEquals(AripickStatus.거절됨, result.status)
    }

    @Test
    fun `delete throws when requester is not proposer`() {
        val repository = FakeAripickRepository(
            initialItems = listOf(aripickFixture(proposalId = 1L, proposerId = 2L)),
        )
        val useCase = AripickCommandUseCase(repository, FakeAripickPolicyRepository(), FakeFoodSafetyRepository(), AripickMapper())

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
        val useCase = AripickCommandUseCase(repository, FakeAripickPolicyRepository(), FakeFoodSafetyRepository(), AripickMapper())

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
        val useCase = AripickCommandUseCase(repository, FakeAripickPolicyRepository(), FakeFoodSafetyRepository(), AripickMapper())

        useCase.deleteAsAdmin(1L)

        assertTrue(repository.deletedProposalIds.contains(1L))
    }

    @Test
    fun `toggle like stores like and increments count when not liked`() {
        val repository = FakeAripickRepository(
            initialItems = listOf(aripickFixture(proposalId = 1L, like = 0)),
        )
        val useCase = AripickCommandUseCase(repository, FakeAripickPolicyRepository(), FakeFoodSafetyRepository(), AripickMapper())

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
        val useCase = AripickCommandUseCase(repository, FakeAripickPolicyRepository(), FakeFoodSafetyRepository(), AripickMapper())

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
        val useCase = AripickCommandUseCase(repository, FakeAripickPolicyRepository(), FakeFoodSafetyRepository(), AripickMapper())

        val result = useCase.pending(1L)

        assertEquals(AripickStatus.검토중, result.status)
    }

    @Test
    fun `command throws when proposal does not exist`() {
        val useCase = AripickCommandUseCase(
            FakeAripickRepository(),
            FakeAripickPolicyRepository(),
            FakeFoodSafetyRepository(),
            AripickMapper(),
        )

        assertThrows(AripickNotFoundException::class.java) {
            useCase.approve(100L)
        }
    }
}
