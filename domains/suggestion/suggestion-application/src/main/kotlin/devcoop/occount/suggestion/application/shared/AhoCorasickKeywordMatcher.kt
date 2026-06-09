package devcoop.occount.suggestion.application.shared

import devcoop.occount.suggestion.application.output.AripickPolicyRepository
import devcoop.occount.suggestion.domain.aripick.AripickPolicyUnavailableException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import org.springframework.stereotype.Component

@Component
class AhoCorasickKeywordMatcher(
    private val aripickPolicyRepository: AripickPolicyRepository,
) {
    private val refreshRetryBackoffMillis = 5_000L
    private val automatonRef = AtomicReference(AhoCorasickAutomaton.empty())
    private val initializedRef = AtomicReference(false)
    private val refreshingRef = AtomicBoolean(false)
    private val nextRefreshAllowedAtMillisRef = AtomicLong(0L)

    fun refresh() {
        val now = System.currentTimeMillis()
        if (now < nextRefreshAllowedAtMillisRef.get()) {
            return
        }
        if (!refreshingRef.compareAndSet(false, true)) {
            return
        }

        runCatching {
            val keywords = aripickPolicyRepository.findBlockedKeywords()
                .map { it.getKeyword() }
            automatonRef.set(AhoCorasickAutomaton.build(keywords))
            initializedRef.set(true)
            nextRefreshAllowedAtMillisRef.set(0L)
        }.onFailure {
            nextRefreshAllowedAtMillisRef.set(System.currentTimeMillis() + refreshRetryBackoffMillis)
        }.also {
            refreshingRef.set(false)
        }
    }

    fun contains(text: String): Boolean {
        if (!initializedRef.get()) {
            refresh()
            if (!initializedRef.get()) {
                throw AripickPolicyUnavailableException()
            }
        }
        return automatonRef.get().contains(text)
    }
}
