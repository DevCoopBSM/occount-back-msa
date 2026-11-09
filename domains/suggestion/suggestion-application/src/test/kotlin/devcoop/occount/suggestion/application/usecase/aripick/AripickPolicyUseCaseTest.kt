package devcoop.occount.suggestion.application.usecase.aripick

import devcoop.occount.suggestion.application.shared.AhoCorasickKeywordMatcher
import devcoop.occount.suggestion.application.support.FakeAripickPolicyRepository
import devcoop.occount.suggestion.domain.aripick.AripickBlockedKeywordAlreadyExistsException
import devcoop.occount.suggestion.domain.aripick.AripickInvalidBlockedKeywordException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AripickPolicyUseCaseTest {
    @Test
    fun `block and list keywords`() {
        val policyRepository = FakeAripickPolicyRepository()
        val useCase = AripickPolicyUseCase(
            aripickPolicyRepository = policyRepository,
            blockedKeywordMatcher = AhoCorasickKeywordMatcher(policyRepository),
        )

        useCase.blockKeyword(CreateAripickBlockedKeywordRequest(keyword = "에너지"))
        val result = useCase.getBlockedKeywords()

        assertEquals(1, result.keywords.size)
        assertEquals("에너지", result.keywords.first().keyword)
    }

    @Test
    fun `unblock keyword removes it`() {
        val policyRepository = FakeAripickPolicyRepository()
        val useCase = AripickPolicyUseCase(
            aripickPolicyRepository = policyRepository,
            blockedKeywordMatcher = AhoCorasickKeywordMatcher(policyRepository),
        )
        val created = useCase.blockKeyword(CreateAripickBlockedKeywordRequest(keyword = "에너지"))

        useCase.unblockKeyword(created.keywordId)
        val result = useCase.getBlockedKeywords()

        assertEquals(0, result.keywords.size)
    }

    @Test
    fun `block keyword throws when keyword is blank`() {
        val policyRepository = FakeAripickPolicyRepository()
        val useCase = AripickPolicyUseCase(
            aripickPolicyRepository = policyRepository,
            blockedKeywordMatcher = AhoCorasickKeywordMatcher(policyRepository),
        )

        assertThrows(AripickInvalidBlockedKeywordException::class.java) {
            useCase.blockKeyword(CreateAripickBlockedKeywordRequest(keyword = "   "))
        }
    }

    @Test
    fun `block keyword throws when keyword already exists`() {
        val policyRepository = FakeAripickPolicyRepository()
        val useCase = AripickPolicyUseCase(
            aripickPolicyRepository = policyRepository,
            blockedKeywordMatcher = AhoCorasickKeywordMatcher(policyRepository),
        )
        useCase.blockKeyword(CreateAripickBlockedKeywordRequest(keyword = "에너지"))

        assertThrows(AripickBlockedKeywordAlreadyExistsException::class.java) {
            useCase.blockKeyword(CreateAripickBlockedKeywordRequest(keyword = "  에너지 "))
        }
    }
}
