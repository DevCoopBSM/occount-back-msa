package devcoop.occount.suggestion.application.shared

import devcoop.occount.suggestion.application.support.FakeAripickPolicyRepository
import devcoop.occount.suggestion.domain.aripick.AripickBlockedKeyword
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AhoCorasickKeywordMatcherTest {
    @Test
    fun `automaton contains returns true when one keyword is included`() {
        val automaton = AhoCorasickAutomaton.build(listOf("사이다", "에너지", "콜라"))

        val result = automaton.contains("에너지 드링크 제로")

        assertTrue(result)
    }

    @Test
    fun `automaton contains is case insensitive and ignores blanks`() {
        val automaton = AhoCorasickAutomaton.build(listOf("   ", "sugar"))

        val result = automaton.contains("Zero Sugar Drink")

        assertTrue(result)
    }

    @Test
    fun `automaton contains returns false when no keyword matches`() {
        val automaton = AhoCorasickAutomaton.build(listOf("환타", "사이다"))

        val result = automaton.contains("제로 콜라")

        assertFalse(result)
    }

    @Test
    fun `matcher refresh reflects newly blocked keyword`() {
        val policyRepository = FakeAripickPolicyRepository()
        val matcher = AhoCorasickKeywordMatcher(policyRepository)

        assertFalse(matcher.contains("에너지 드링크 제로"))

        policyRepository.saveBlockedKeyword("에너지")
        matcher.refresh()

        assertTrue(matcher.contains("에너지 드링크 제로"))
    }

    @Test
    fun `matcher refresh removes unblocked keyword`() {
        val policyRepository = FakeAripickPolicyRepository(
            initialKeywords = listOf(AripickBlockedKeyword(keywordId = 1L, keyword = "에너지")),
        )
        val matcher = AhoCorasickKeywordMatcher(policyRepository)

        assertTrue(matcher.contains("에너지 드링크 제로"))

        policyRepository.deleteBlockedKeyword(1L)
        matcher.refresh()

        assertFalse(matcher.contains("에너지 드링크 제로"))
    }
}
