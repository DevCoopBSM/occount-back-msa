package devcoop.occount.suggestion.application.shared

import devcoop.occount.suggestion.application.output.AripickPolicyRepository
import java.util.concurrent.atomic.AtomicReference
import org.springframework.stereotype.Component

@Component
class AhoCorasickKeywordMatcher(
    private val aripickPolicyRepository: AripickPolicyRepository,
) {
    private val automatonRef = AtomicReference(AhoCorasickAutomaton.empty())
    private val initializedRef = AtomicReference(false)

    fun refresh() {
        runCatching {
            val keywords = aripickPolicyRepository.findBlockedKeywords()
                .map { it.getKeyword() }
            automatonRef.set(AhoCorasickAutomaton.build(keywords))
            initializedRef.set(true)
        }.onFailure {
            // Keep serving with the last successful snapshot (or empty on cold start).
        }
    }

    fun contains(text: String): Boolean {
        if (!initializedRef.get()) {
            refresh()
        }
        return automatonRef.get().contains(text)
    }
}
