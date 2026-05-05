package devcoop.occount.suggestion.application.shared

import devcoop.occount.suggestion.application.output.AripickPolicyRepository
import java.util.concurrent.atomic.AtomicReference
import org.springframework.stereotype.Component

@Component
class AhoCorasickKeywordMatcher(
    private val aripickPolicyRepository: AripickPolicyRepository,
) {
    private val automatonRef = AtomicReference(AhoCorasickAutomaton.empty())

    init {
        refresh()
    }

    fun refresh() {
        val keywords = aripickPolicyRepository.findBlockedKeywords()
            .map { it.getKeyword() }
        automatonRef.set(AhoCorasickAutomaton.build(keywords))
    }

    fun contains(text: String): Boolean {
        return automatonRef.get().contains(text)
    }
}
