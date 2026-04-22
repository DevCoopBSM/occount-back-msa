package devcoop.occount.suggestion.application.usecase.aripick

import devcoop.occount.suggestion.application.support.FakeAripickPolicyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AripickPolicyUseCaseTest {
    @Test
    fun `block and list keywords`() {
        val useCase = AripickPolicyUseCase(FakeAripickPolicyRepository())

        useCase.blockKeyword(CreateAripickBlockedKeywordRequest(keyword = "에너지"))
        val result = useCase.getBlockedKeywords()

        assertEquals(1, result.keywords.size)
        assertEquals("에너지", result.keywords.first().keyword)
    }

    @Test
    fun `unblock keyword removes it`() {
        val useCase = AripickPolicyUseCase(FakeAripickPolicyRepository())
        val created = useCase.blockKeyword(CreateAripickBlockedKeywordRequest(keyword = "에너지"))

        useCase.unblockKeyword(created.keywordId)
        val result = useCase.getBlockedKeywords()

        assertEquals(0, result.keywords.size)
    }
}
